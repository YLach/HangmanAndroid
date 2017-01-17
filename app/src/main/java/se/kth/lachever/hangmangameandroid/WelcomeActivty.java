package se.kth.lachever.hangmangameandroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class WelcomeActivty extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        setupGUI();
    }

    public void setupGUI() {
        Button connectButton = (Button)findViewById(R.id.connection_button);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WelcomeActivty.this, ServerConnectionActivity.class);
                startActivity(intent);
            }
        });
    }



}
