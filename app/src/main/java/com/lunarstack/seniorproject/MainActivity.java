package com.lunarstack.seniorproject;

import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.ArrayList;
import java.util.Random;

import static com.google.android.gms.nearby.connection.Payload.fromBytes;

/** A class that connects to Nearby Connections and provides convenience methods and callbacks. */
public class MainActivity extends ConnectionsActivity {

    private final String TAG = "com.lunarstack.com.seniorproject.MainActivity";
    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    private static final String SERVICE_ID =
            "com.lunarstack.com.seniorproject.SERVICE_ID";
    /**
     * The state of the app. As the app changes states, the UI will update and advertising/discovery
     * will start/stop.
     */
    private State mState = State.UNKNOWN;
    private String mName;

    private EditText mSendEditText;
    private Button mSendButton;
    private ListView mMessagesListView;

    private String mDestructCode;

    private ArrayList<String> mMessages;
    private ArrayAdapter<String> mMessagesListAdapter;

    private final SimpleArrayMap<Long, NotificationCompat.Builder> incomingPayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, NotificationCompat.Builder> outgoingPayloads = new SimpleArrayMap<>();



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSendEditText = (EditText) findViewById(R.id.sendEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);
        mMessagesListView = (ListView) findViewById(R.id.messagesList);

        mMessages = new ArrayList<>();
        mMessagesListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mMessages);
        mMessagesListView.setAdapter(mMessagesListAdapter);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String temp = mSendEditText.getText().toString();
                // if there is text to send, send it to our peer
                if(temp != null) {
                    // initialize a buffer to read in the first character of the edit box...if it is
                    // an exclamation point we will read the following text in as the self destruct
                    // value.
                    char buf[] = new char[1];
                    temp.getChars(0,0, buf, 0);
                    char first = buf[0];

                    if(Character.toString(first).equals("!")) {
                        mDestructCode = temp.substring(1, temp.length()); // set our destruct code
                    }

                    byte[] array = temp.getBytes();
                    send(fromBytes(array));
                    mSendEditText.setText(""); // clear textbox
                    mMessages.add(temp);

                    mMessagesListAdapter.notifyDataSetChanged();
                }
            }
        });

        mName = generateRandomName();
        setState(State.SEARCHING);
    }


    @Override
    protected void onEndpointDiscovered(Endpoint endpoint) {
        // We found an advertiser!
        stopDiscovering();
        connectToEndpoint(endpoint);
    }

    @Override
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        // A connection to another device has been initiated! We'll use the auth token, which is the
        // same on both devices, to pick a color to use when we're connected. This way, users can
        // visually see which device they connected with.
        // We accept the connection immediately.
        acceptConnection(endpoint);
    }

    @Override
    protected void onEndpointConnected(Endpoint endpoint) {
        Toast.makeText(
                this, getString(R.string.toast_connected, endpoint.getName()), Toast.LENGTH_SHORT)
                .show();
        setState(State.CONNECTED);
        /*String temp = "hello";
        byte[] array = temp.getBytes();
        send(fromBytes(array));*/
    }

    @Override
    protected void onEndpointDisconnected(Endpoint endpoint) {
        Toast.makeText(
                this, getString(R.string.toast_disconnected, endpoint.getName()), Toast.LENGTH_SHORT)
                .show();
        setState(State.SEARCHING);
    }

    @Override
    protected void onConnectionFailed(Endpoint endpoint) {
        // Let's try someone else.
        if (getState() == State.SEARCHING) {
            startDiscovering();
        }
    }

    @Override
    protected void onReceive(Endpoint endpoint, Payload payload) {
        if(payload.getType() == Payload.Type.BYTES) {
            String message = new String(payload.asBytes());
            Log.d(TAG, "Payload from " + endpoint + " -- " + message);
            mMessages.add(message);
            mMessagesListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        setState(State.SEARCHING);
    }

    @Override
    protected String getName() {
        return mName;
    }

    @Override
    protected String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    protected Strategy getStrategy() {
        return STRATEGY;
    }

    private void onStateChanged(State state) {
        switch(state) {
            case SEARCHING:
                disconnectFromAllEndpoints();
                startDiscovering();
                startAdvertising();
                break;
            case CONNECTED:
                stopAdvertising();
                startDiscovering();
                break;
            case UNKNOWN:
                stopAllEndpoints();
                break;

            default:
                // no-op
                break;
        }
    }

    private static String generateRandomName() {
        String name = "";
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            name += random.nextInt(10);
        }
        return name;
    }

    /**
     * The state has changed. I wonder what we'll be doing now.
     *
     * @param state The new state.
     */
    private void setState(State state) {
        if (mState == state) {
            Log.d(TAG, "State set to " + state + " but already in that state");
            return;
        }

        logD("State set to " + state);
        mState = state;
        ((TextView) findViewById(R.id.stateTextView)).setText(mName + " -- " + state);
        onStateChanged(state);
    }

    /** @return The current state. */
    private State getState() {
        return mState;
    }


    /** States that the UI goes through. */
    public enum State {
        UNKNOWN,
        SEARCHING,
        ADVERTISING,
        CONNECTED
    }
}