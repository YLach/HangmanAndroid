package se.kth.lachever.hangmangameandroid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class ServerConnectionActivity extends Activity {
    private static final String TAG = "ServerConnectionAct";
    private final static int IP_ADDRESS_INDEX = 0;
    private final static int PORT_INDEX = 1;

    public final static String SRV_HOST = "se.kth.lachever.hangmangameandroid.SRV_HOST";
    public final static String SRV_PORT = "se.kth.lachever.hangmangameandroid.SRV_PORT";

    private static Context mContext;
    protected EditText srvAddress;
    protected EditText srvPort;
    protected Button connectButton;

    // Service : server connection
    private ServerConnectionService connectionService;
    private boolean isBound = false;

    // To exchange messages between the activity and the service
    LocalBroadcastManager bManager;
    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent intentGameActivity = new Intent(getAppContext(), GameActivity.class);
            startActivity(intentGameActivity);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getApplicationContext();
        setContentView(R.layout.activity_connection);

        // BroadcastManager
        bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter(ServerConnectionService.SRV_CONNECTED);
        bManager.registerReceiver(bReceiver, intentFilter);

        // GUI
        setupGUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bManager.unregisterReceiver(bReceiver);
        doUnbindService();
    }

    public void setupGUI() {
        connectButton = (Button) findViewById(R.id.connection_button);
        connectButton.setEnabled(false);

        srvAddress = (EditText) findViewById(R.id.ipAddress_edittext);
        srvPort = (EditText) findViewById(R.id.port_edittext);

        TextWatcher tw = new TextEditWatcher();
        srvAddress.addTextChangedListener(tw);
        srvPort.addTextChangedListener(tw);


        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ServerConnectionActivity.this,
                        ServerConnectionService.class);
                intent.putExtra(SRV_HOST, srvAddress.getText().toString());
                intent.putExtra(SRV_PORT, Integer.parseInt(srvPort.getText().toString()));
                startService(intent);
                doBindService();
            }
        });
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            connectionService = ((ServerConnectionService.LocalBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            connectionService = null;
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(ServerConnectionActivity.this,
                ServerConnectionService.class), mConnection, Context.BIND_AUTO_CREATE);
        isBound = true;
    }

    void doUnbindService() {
        if (isBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            isBound = false;
        }
    }

    public static Context getAppContext() {
        return mContext;
    }


    private class TextEditWatcher implements TextWatcher{
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        public void afterTextChanged(Editable editable) {
            if (srvAddress.getText().toString().trim().length() == 0 ||
                    srvPort.getText().toString().trim().length() == 0) {
                connectButton.setEnabled(false);
            } else {
                connectButton.setEnabled(true);
            }
        }
    }
}
