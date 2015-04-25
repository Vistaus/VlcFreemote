package com.nico.vlcfremote.utils;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class HttpUtils {

    public static interface XmlMogrifierCallback<T> {
        void reset();
        void parseValue(final String name, final String value);
        T getParsedObject();
    }

    public static <T> List<T> parseXmlList(final String xmlMsg, final String interestingTag, XmlMogrifierCallback<T> callback) {
        List<T> foundObjects = new ArrayList<>();

        final XmlPullParser xpp;
        try {
            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance(); // TODO Static'ize?
            xpp = factory.newPullParser();
            xpp.setInput( new StringReader(xmlMsg) );
        } catch (XmlPullParserException e) {
            Log.e("ASD", "Can't setup XML parser");
            return foundObjects; // TODO: Notify user, throw something
        }
        try {
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_TAG && (xpp.getName().equals(interestingTag))) {
                    callback.reset();

                    for (int i=0; i < xpp.getAttributeCount(); ++i) {
                        callback.parseValue(xpp.getAttributeName(i), xpp.getAttributeValue(i));
                    }

                    foundObjects.add(callback.getParsedObject());
                }

                eventType = xpp.next();
            }
        } catch (XmlPullParserException e) {
            Log.e("ASD", "Can't read XML");
            return foundObjects; // TODO: Notify user, throw something
        } catch (IOException e) {
            Log.e("ASD", "Can't read XML");
            return foundObjects; // TODO: Notify user, throw something
        }

        return foundObjects;
    }


    public static interface HttpResponseCallback {
        void responseReceived(final String msg);
    }

    public static class AsyncRequester extends AsyncTask<Void, Void, String> {
        private final HttpClient httpClient;
        private final HttpGet op;
        private final HttpResponseCallback callback;

        public AsyncRequester(HttpClient httpClient, HttpGet op, HttpResponseCallback callback) {
            this.httpClient = httpClient;
            this.op = op;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... params) {
            String responseString;
            try {
                HttpResponse resp = httpClient.execute(op);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                resp.getEntity().writeTo(out);
                responseString = out.toString();
                out.close();
                return responseString;
            } catch (IOException e) {
                e.printStackTrace(); // TODO
                return null;
            }
        }

        @Override
        protected void onPostExecute(final String response) {
            callback.responseReceived(response);
        }
    }
}
