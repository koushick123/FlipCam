package com.flipcam.util;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by Koushick on 12-08-2017.
 */

public class GLUtil {
    public static final String TAG = "GLUtil";
    static boolean VERBOSE = true;
    /*
    Copyright 2014 Google Inc. All rights reserved.
    Borrowed from Grafika project. This is NOT an official Google Project,
    and has an Open Source license.
    https://github.com/google/grafika
     */
    // Simple vertex shader, used for all programs.
    public static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    // Simple fragment shader for use with external 2D textures (e.g. what we get from
    // SurfaceTexture).
    public static final String FRAGMENT_SHADER_EXT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform float changer;\n" +
                    "float r, g, b;\n" +
                    "void main() {\n" +
                    "    vec4 tex = texture2D(sTexture, vTextureCoord);\n" +
                    "    r = clamp(tex.r + changer, 0.0, 1.0);\n" +
                    "    g = clamp(tex.g + changer, 0.0, 1.0);\n" +
                    "    b = clamp(tex.b + changer, 0.0, 1.0);\n" +
                    "    gl_FragColor = vec4(r, g, b, tex.a);\n" +
                    "}\n";
    /**
     * A "full" square, extending from -1 to +1 in both dimensions.  When the model/view/projection
     * matrix is identity, this will exactly cover the viewport.
     * <p>
     * The texture coordinates are Y-inverted relative to RECTANGLE.  (This seems to work out
     * right with external textures from SurfaceTexture.)
     */
    public static final float FULL_RECTANGLE_COORDS[] = {
            -1.0f, -1.0f,   // 0 bottom left
            1.0f, -1.0f,   // 1 bottom right
            -1.0f,  1.0f,   // 2 top left
            1.0f,  1.0f,   // 3 top right
    };
    public static final float FULL_RECTANGLE_TEX_COORDS[] = {
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
    };

    private static final int SIZEOF_FLOAT = 4;
    private static int mProgramHandle;
    private static int mTextureTarget;
    private static int mTextureId;
    private static int muMVPMatrixLoc;
    private static int muTexMatrixLoc;
    private static int maPositionLoc;
    private static int maTextureCoordLoc;
    private static EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private static EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private static EGLConfig mEGLConfig = null;
    //Surface onto which camera frames are drawn
    private static EGLSurface eglSurface;
    private static SurfaceTexture surfaceTexture;
    // Android-specific extension.
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;
    public static final int FLAG_RECORDABLE = 0x01;
    private GLUtil() {}     // do not instantiate

    /**
     * Creates a new program from the supplied vertex and fragment shaders.
     *
     * @return A handle to the program, or 0 on failure.
     */
    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        checkGlError("glCreateProgram");
        if (program == 0) {
            if(VERBOSE)Log.e(TAG, "Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            if(VERBOSE)Log.e(TAG, "Could not link program: ");
            if(VERBOSE)Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    /**
     * Compiles the provided shader source.
     *
     * @return A handle to the shader, or 0 on failure.
     */
    public static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGlError("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            if(VERBOSE)Log.e(TAG, "Could not compile shader " + shaderType + ":");
            if(VERBOSE)Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    /**
     * Checks to see if a GLES error has been raised.
     */
    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            if(VERBOSE)Log.e(TAG, msg);
            throw new RuntimeException(msg);
        }
    }

    /**
     * Checks to see if the location we obtained is valid.  GLES returns -1 if a label
     * could not be found, but does not set the GL error.
     * <p>
     * Throws a RuntimeException if the location is invalid.
     */
    public static void checkLocation(int location, String label) {
        if (location < 0) {
            throw new RuntimeException("Unable to locate '" + label + "' in program");
        }
    }

    /**
     * Allocates a direct float buffer, and populates it with the float array data.
     */
    public static FloatBuffer createFloatBuffer(float[] coords) {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    /**
     * Copyright 2014 Google Inc. All rights reserved.
     Borrowed from Grafika project. This is NOT an official Google Project,
     and has an Open Source license.
     https://github.com/google/grafika
     * Creates a texture object suitable for use with this program.
     * <p>
     * On exit, the texture will be bound.
     */
    public static int createGLTextureObject(int mTextureTarget) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        checkGlError("glGenTextures");

        int texId = textures[0];
        GLES20.glBindTexture(mTextureTarget, texId);
        checkGlError("glBindTexture " + texId);

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        checkGlError("glTexParameter");
        return texId;
    }

    public static EGLSurface prepareWindowSurface(Surface surface, EGLDisplay mEGLDisplay, EGLConfig mEGLConfig)
    {
        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };
        EGLSurface windowSurface;
        windowSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, surface , surfaceAttribs, 0);
        checkEglError("eglCreateWindowSurface");
        if (windowSurface == null) {
            throw new RuntimeException("surface was null");
        }
        return windowSurface;
    }

    public static void checkEglError(String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    private static int changer;
    private static int tex;
    public static float colorVal = 0.0f;

    public static void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        GLUtil.surfaceTexture = surfaceTexture;
    }

    public static SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    /*
        Copyright 2014 Google Inc. All rights reserved.
         Borrowed from Grafika project. This is NOT an official Google Project,
         and has an Open Source license.
         https://github.com/google/grafika
         */
    public static void createSurfaceTexture(SurfaceHolder surfaceHolder, EGLDisplay mEGLDisplay, EGLConfig mEGLConfig)
    {
        eglSurface = prepareWindowSurface(surfaceHolder.getSurface(), mEGLDisplay, mEGLConfig);
        makeCurrent(eglSurface);
        mProgramHandle = createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT);
        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        checkLocation(maPositionLoc, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        checkLocation(maTextureCoordLoc, "aTextureCoord");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        checkLocation(muMVPMatrixLoc, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        checkLocation(muTexMatrixLoc, "uTexMatrix");
        changer = GLES20.glGetUniformLocation(mProgramHandle, "changer");
        checkLocation(changer, "changer");
        tex = GLES20.glGetUniformLocation(mProgramHandle, "sTexture");
        checkLocation(tex, "sTexture");
        mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
        mTextureId = createGLTextureObject(mTextureTarget);
        surfaceTexture = new SurfaceTexture(mTextureId);
    }

    /**
     * Copyright 2014 Google Inc. All rights reserved.
     Borrowed from Grafika project. This is NOT an official Google Project,
     and has an Open Source license.
     https://github.com/google/grafika

     * Issues the draw call.  Does the full setup on every call.
     *
     * @param mvpMatrix The 4x4 projection matrix.
     * @param vertexBuffer Buffer with vertex position data.
     * @param firstVertex Index of first vertex to use in vertexBuffer.
     * @param vertexCount Number of vertices in vertexBuffer.
     * @param coordsPerVertex The number of coordinates per vertex (e.g. x,y is 2).
     * @param vertexStride Width, in bytes, of the position data for each vertex (often
     *        vertexCount * sizeof(float)).
     * @param texMatrix A 4x4 transformation matrix for texture coords.  (Primarily intended
     *        for use with SurfaceTexture.)
     * @param texBuffer Buffer with vertex texture data.
     * @param texStride Width, in bytes, of the texture data for each vertex.
     */
    public static void draw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex,
                      int vertexCount, int coordsPerVertex, int vertexStride,
                      float[] texMatrix, FloatBuffer texBuffer, int texStride) {
        checkGlError("draw start");

        //tex = GLES20.glGetUniformLocation(mProgramHandle, "sTexture");
        //changer = GLES20.glGetUniformLocation(mProgramHandle, "changer");
        // Select the program.
        GLES20.glUseProgram(mProgramHandle);
        checkGlError("glUseProgram");

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureTarget, mTextureId);

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
        checkGlError("glUniformMatrix4fv");

        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
        checkGlError("glUniformMatrix4fv");
        GLES20.glUniform1i(tex, 0);
        GLES20.glUniform1f(changer, colorVal);
        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        checkGlError("glEnableVertexAttribArray");

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        checkGlError("glVertexAttribPointer");

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        checkGlError("glEnableVertexAttribArray");

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false, texStride, texBuffer);
        checkGlError("glVertexAttribPointer");

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
        checkGlError("glDrawArrays");

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(mTextureTarget, 0);
        GLES20.glUseProgram(0);
    }

    public static EGLDisplay getmEGLDisplay() {
        return mEGLDisplay;
    }

    public static void setmEGLConfig(EGLConfig mEGLConfig) {
        GLUtil.mEGLConfig = mEGLConfig;
    }

    public static EGLContext getmEGLContext() {
        return mEGLContext;
    }

    public static void setmEGLContext(EGLContext mEGLContext) {
        GLUtil.mEGLContext = mEGLContext;
    }

    public static void setmEGLDisplay(EGLDisplay mEGLDisplay) {
        GLUtil.mEGLDisplay = mEGLDisplay;
    }

    public static EGLConfig getmEGLConfig() {
        return mEGLConfig;
    }

    public static void prepareEGLDisplayandContext()
    {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null;
            throw new RuntimeException("unable to initialize EGL14");
        }
        EGLConfig config = getConfig(FLAG_RECORDABLE, 2);
        if (config == null) {
            throw new RuntimeException("Unable to find a suitable EGLConfig");
        }
        int[] attrib2_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        EGLContext context = EGL14.eglCreateContext(mEGLDisplay, config, EGL14.EGL_NO_CONTEXT,
                attrib2_list, 0);
        checkEglError("eglCreateContext");
        mEGLConfig = config;
        mEGLContext = context;

        // Confirm with query.
        int[] values = new int[1];
        EGL14.eglQueryContext(mEGLDisplay, mEGLContext, EGL14.EGL_CONTEXT_CLIENT_VERSION,
                values, 0);
        if(VERBOSE)Log.d(TAG, "EGLContext created, client version " + values[0]);
    }

    public static EGLConfig getConfig(int flags, int version) {
        int renderableType = EGL14.EGL_OPENGL_ES2_BIT;

        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, renderableType,
                EGL14.EGL_NONE, 0,      // placeholder for recordable [@-3]
                EGL14.EGL_NONE
        };
        if ((flags & FLAG_RECORDABLE) != 0) {
            attribList[attribList.length - 3] = EGL_RECORDABLE_ANDROID;
            attribList[attribList.length - 2] = 1;
        }
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
                numConfigs, 0)) {
            Log.w(TAG, "unable to find RGB8888 / " + version + " EGLConfig");
            return null;
        }
        return configs[0];
    }

    public static int getmProgramHandle() {
        return mProgramHandle;
    }

    public static EGLSurface getEglSurface() {
        return eglSurface;
    }

    public static void makeCurrent(EGLSurface surface)
    {
        EGL14.eglMakeCurrent(mEGLDisplay, surface, surface, mEGLContext);
    }
}
