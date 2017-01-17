package se.kth.lachever.hangmangameandroid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;


public class GameActivity extends Activity {

    private ServerConnectionService connectionService;
    private boolean isBound = false;
    private Game game;

    // GUI
    private List<Button> letterButtons = new LinkedList<>();
    private TextView wordToGuessTV;
    private TextView scoreTV;
    private TextView failedAttemptsTV;
    private EditText wordPropTextField;
    private Button validatePropButton;
    private Button newGameButton;
    private TextView gameStatus;


    // To exchange messages between the activity and the service
    LocalBroadcastManager bManager;
    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String[] fromServer = intent.getStringExtra(ServerConnectionService.SRV_MESSAGE_CONTENT).split("\\+");

            System.out.println("From Server : " + intent.getStringExtra(ServerConnectionService.SRV_MESSAGE_CONTENT)); // TODO : to remove

            if (fromServer[0].equals(Game.GAME_OVER)) {
                // Client loses
                wordPropTextField.setEnabled(false);
                validatePropButton.setEnabled(false);
                for (Button b : letterButtons)
                    b.setEnabled(false);

                game.setScore(Integer.parseInt(fromServer[1]));
                game.setStatusToFinished();
                gameStatus.setText("Game Over !");
                gameStatus.setTextColor(Color.RED);
                gameStatus.setEnabled(true);
            } else if (fromServer[0].equals(Game.GAME_WIN)) {
                // Client wins
                wordPropTextField.setEnabled(false);
                validatePropButton.setEnabled(false);
                for (Button b : letterButtons)
                    b.setEnabled(false);

                game.modifyCurrentViewOfWord(fromServer[1]);
                game.setStatusToFinished();
                game.setScore(Integer.parseInt(fromServer[2]));
                gameStatus.setText("Congratulation, you win !");
                gameStatus.setTextColor(Color.GREEN);
                gameStatus.setEnabled(true);
            } else {
                game.modifyCurrentViewOfWord(fromServer[0]);
                game.setNbFailedAttempts(Integer.parseInt(fromServer[1]));
            }
            updateGUI();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // BroadcastManager
        bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter(ServerConnectionService.SRV_MESSAGE);
        bManager.registerReceiver(bReceiver, intentFilter);

        // Bind service
        doBindService();

        game = new Game();
        setupGUI();
        updateGUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bManager.unregisterReceiver(bReceiver);
        doUnbindService();
    }

    public void setupGUI() {
        GridLayout lettersGrid = (GridLayout) findViewById(R.id.letters_grid);
        lettersGrid.removeAllViews();

        wordToGuessTV = (TextView) findViewById(R.id.wordToGuess);
        scoreTV = (TextView) findViewById(R.id.score_value);
        failedAttemptsTV = (TextView) findViewById(R.id.failedAttempts_value);
        wordPropTextField = (EditText) findViewById(R.id.wordGuessed_edittext);
        validatePropButton = (Button) findViewById(R.id.wordValidation_button);
        newGameButton = (Button) findViewById(R.id.newGame_button);
        gameStatus = (TextView) findViewById(R.id.gameStatus);


        //int row = (int) Math.ceil(26 / (lettersGrid.getWidth() * 60));
        int row = 6;
        //int elemsPerRow = lettersGrid.getWidth() / 50;
        int elemsPerRow = 7;

        lettersGrid.setColumnCount(elemsPerRow);
        lettersGrid.setRowCount(row);

        int aCode = (int) 'A';
        for (int i = 0, currentCol = 0, currentRow = 0; i < 26; i++, currentCol++) {
            if (currentCol == elemsPerRow) {
                currentCol = 0;
                currentRow++;
            }

            char letter = (char) (aCode + i); // Corresponding letter
            Button b = new Button(this);
            letterButtons.add(b);
            b.setText(Character.toString(letter));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 175;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params.rightMargin = 5;
            params.rightMargin = 5;
            params.columnSpec = GridLayout.spec(currentCol);
            params.rowSpec = GridLayout.spec(currentRow);
            b.setLayoutParams(params);
            b.setOnClickListener(new LetterButtonListener(letter));
            lettersGrid.addView(b);
        }

        newGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gameStatus.setText("");
                wordPropTextField.setEnabled(true);
                validatePropButton.setEnabled(true);
                for (Button b : letterButtons)
                    b.setEnabled(true);
                wordPropTextField.setText("");
                game.newGame();
                connectionService.sendMessageToServer(game.NEW_GAME);
            }
        });

        validatePropButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectionService.sendMessageToServer(wordPropTextField.getText().toString().toUpperCase());
            }
        });
    }

    private class LetterButtonListener implements View.OnClickListener {
        private final char letter;

        private LetterButtonListener(char letter) {
            this.letter = letter;
        }

        @Override
        public void onClick(View view) {
            connectionService.sendMessageToServer(String.valueOf(letter));
        }
    }


    private void updateGUI() {
        if (!wordToGuessTV.getText().toString().equals(game.getCurrentViewOfWord())) //TODO incompatible types
            wordToGuessTV.setText(game.getCurrentViewOfWord().toString());

        try {
            if (Integer.parseInt(failedAttemptsTV.getText().toString()) != game.getNbFailedAttempts())
                failedAttemptsTV.setText(String.valueOf(game.getNbFailedAttempts()));
        } catch (NumberFormatException n) {
            failedAttemptsTV.setText(String.valueOf(game.getNbFailedAttempts()));
        }

        try {
            if (Integer.parseInt(scoreTV.getText().toString()) != game.getScore())
                scoreTV.setText(String.valueOf(game.getScore()));
        } catch (NumberFormatException n) {
            scoreTV.setText(String.valueOf(game.getScore()));
        }
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
        bindService(new Intent(GameActivity.this,
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
}
