package com.flipcam.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.flipcam.R;
import com.flipcam.SettingsActivity;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class FeedbackMailTask extends AsyncTask<String, Void, Void> {

    static final String TAG = "FeedbackMailTask";
    Context mContext;
    SettingsActivity settingsActivity;
    public FeedbackMailTask(Context context, AppCompatActivity appCompatActivity) {
        super();
        mContext = context;
        settingsActivity = (SettingsActivity)appCompatActivity;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        settingsActivity.showFeedbackMessage();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        settingsActivity.getFeedback_information().setText("");
        settingsActivity.hideFeedbackMessage();
        Toast.makeText(mContext,settingsActivity.getResources().getString(R.string.thanksFeedbackMsg), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected Void doInBackground(String... params) {

        class SMTPAuthenticator extends Authenticator
        {
            public PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication("User", "");
            }
        }

        String d_email = "ksprojectsfeedback@gmail.com";
        String d_uname = "ksprojectsfeedback@gmail.com";
        String d_password = "Titanium56";
        String d_host = "smtp.gmail.com";
        String d_port  = "465";
        String m_to = "ksprojectsfeedback@gmail.com";
        String m_subject = "FlipCam Feedback";
        StringBuffer m_text = new StringBuffer("Hi,\n");
        m_text.append(settingsActivity.getFeedback_information().getText().toString());
        Properties props = new Properties();
        props.put("mail.smtp.user", d_email);
        props.put("mail.smtp.host", d_host);
        props.put("mail.smtp.port", d_port);
        props.put("mail.smtp.starttls.enable","true");
        props.put("mail.smtp.debug", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.socketFactory.port", d_port);
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");

        SMTPAuthenticator auth = new SMTPAuthenticator();
        Session session = Session.getInstance(props, auth);
        session.setDebug(true);
        MimeMessage msg = new MimeMessage(session);
        try {
            msg.setText(m_text.toString());
            msg.setSubject(m_subject);
            msg.setFrom(new InternetAddress(d_email));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(m_to));
            Transport transport = session.getTransport("smtps");
            transport.connect(d_host, 465, d_uname, d_password);
            transport.sendMessage(msg, msg.getAllRecipients());
            transport.close();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
