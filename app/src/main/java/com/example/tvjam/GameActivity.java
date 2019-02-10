package com.example.tvjam;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GameActivity extends AppCompatActivity {


    public static final long timeToAnswer = 20000;
    CountDownTimer countDownTimer;
    public static final long roundCooldown = 2000;
    CountDownTimer roundCooldownTimer;
    public long timeLeft;
    public long playerScore;
    public long jokerScore;
    public long skipScore;

    public int maxNumRounds;
    public int maxMistakes;
    public int currNumRounds;
    public int currMistakes;
    public Boolean roundActive;
    public int correctAnswer;
    public int nextCorrectAnswer;
    public boolean gameover;

    public static final int CRAWLAMOUNT = 25;
    public static String WIKIAURL = "";//https://breakingbad.fandom.com";

    public static int questionGeneratorReturn;


    Bitmap imgBit;

    ArrayList<String> possibleTags = new ArrayList<String>();
    String myGoodMainChar = "";
    String myGoodTag = "Aliases";

    ArrayList<String> answers = new ArrayList<String>();
    ArrayList<String> charsVisited = new ArrayList<String>();
    String constructedQuestion;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("score", playerScore);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
            if(countDownTimer != null) countDownTimer.cancel();
            if(roundCooldownTimer != null) roundCooldownTimer.cancel();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        setTitle("Score: 0");

        Intent myIntent = this.getIntent();
        WIKIAURL = myIntent.getStringExtra("wikiaLink");
        possibleTags = myIntent.getStringArrayListExtra("tags");

        Button btn_Joker = findViewById(R.id.btn_Joker);

        btn_Joker.setActivated(false);
        btn_Joker.setBackgroundTintList(btn_Joker.getResources().getColorStateList(R.color.disabled_background_color));
        btn_Joker.setTextColor(btn_Joker.getResources().getColorStateList(R.color.disabled_background_color));

        Button btn_Skip = findViewById(R.id.btn_Skip);

        btn_Skip.setActivated(false);
        btn_Skip.setBackgroundTintList(btn_Skip.getResources().getColorStateList(R.color.disabled_background_color));
        btn_Skip.setTextColor(btn_Skip.getResources().getColorStateList(R.color.disabled_background_color));

        new GetCharacterList().execute(WIKIAURL + "/api/v1/Articles/Top?expand=1&category=Characters&limit=" + CRAWLAMOUNT, "true");


        currNumRounds = 0;
        roundActive = false;

        final SharedPreferences prefs = this.getSharedPreferences("settings", 0);
        final SharedPreferences.Editor editor = prefs.edit();
        maxNumRounds = prefs.getInt("rounds", 10);
        maxMistakes = prefs.getInt("fails", 5);


        gameManager();


    }

    //
    //TODO: 50 / 50 joker
    //

    public void gameManager() {
        if(currNumRounds < maxNumRounds && !roundActive && currMistakes < maxMistakes) {
            if(jokerScore > 500) {
                Button btn_Joker = findViewById(R.id.btn_Joker);
                findViewById(R.id.btn_Joker).setActivated(true);
                btn_Joker.setBackgroundTintList(btn_Joker.getResources().getColorStateList(R.color.defaultGrey));
                btn_Joker.setTextColor(btn_Joker.getResources().getColorStateList(R.color.black));
            }
            if(skipScore > 600) {
                Button btn_Skip = findViewById(R.id.btn_Skip);
                findViewById(R.id.btn_Skip).setActivated(true);
                btn_Skip.setBackgroundTintList(btn_Skip.getResources().getColorStateList(R.color.defaultGrey));
                btn_Skip.setTextColor(btn_Skip.getResources().getColorStateList(R.color.black));
            }
            startNewRound();

        }
        else if(currMistakes >= maxMistakes || currNumRounds >= maxNumRounds) {
            displayGameover();
            gameover = true;

            final SharedPreferences prefs = this.getSharedPreferences("settings", 0);
            final SharedPreferences.Editor editor = prefs.edit();
            String playerName = prefs.getString("playerName", "name");



            //HashMap<String,Object> result = new ObjectMapper().readValue(<JSON_OBJECT>, HashMap.class);


            Map<String, Long> leaderboard;
            //leaderboard.put("test", 1111L);
            //writeMap("leaderboard", leaderboard);

            leaderboard = readMap("leaderboard");

            long currTime = System.currentTimeMillis() / 1000L;

            leaderboard.put(playerName + Long.toString(currTime), playerScore);

           /* leaderboard.put("test1", 1111L);
            leaderboard.put("test2", 2222L);
            leaderboard.put("test3", 11L);
            leaderboard.put("test4", 12111L);
            leaderboard.put("test5", 5111L);
            leaderboard.put("test6", 1L);
            leaderboard.put("test7", 1143411L);*/

            /*Map<String, Long> sorted = leaderboard
                    .entrySet()
                    .stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .collect(
                            toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                    LinkedHashMap::new));



            Log.d("board", leaderboard.toString());
            Log.d("sorted", sorted.toString());*/

            writeMap("leaderboard", leaderboard);
        }
    }

    public void writeMap(String filename, Map<String, Long> myObj){
        File directory = getFilesDir(); //or getExternalFilesDir(null); for external storage
        File file = new File(directory, filename);

        try {
            FileOutputStream fOut = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fOut);
            out.writeObject(myObj);
            out.close();
            fOut.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Long> readMap(String filename) {

        File directory = getFilesDir(); //or getExternalFilesDir(null); for external storage
        File file = new File(directory, filename);

        Map<String, Long> myObj = new HashMap();

        try {
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            myObj = (Map<String, Long>) in.readObject();
            in.close();
            fileIn.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return myObj;
    }

    public void startNewRound() {
        while(answers.size() != 4) {}
        if(currNumRounds != 0) {
            setNewQuestion(constructedQuestion, answers.get(0), answers.get(1), answers.get(2), answers.get(3));
            correctAnswer = nextCorrectAnswer;
            startLevelTimer();
            new GetCharacterList().execute(WIKIAURL + "/api/v1/Articles/Top?expand=1&category=Characters&limit=" + CRAWLAMOUNT, "false");

        }
        currNumRounds++;
        roundActive = true;
        resetButtons();
        String question = "This is a sample question";
        String answer1 = "Answer1";
        String answer2 = "Answer2";
        String answer3 = "Answer3";
        String answer4 = "Answer4";
        //correctAnswer = 2;


        Button btn_Answer1 = findViewById(R.id.btn_Answer1);
        Button btn_Answer2 = findViewById(R.id.btn_Answer2);
        Button btn_Answer3 = findViewById(R.id.btn_Answer3);
        Button btn_Answer4 = findViewById(R.id.btn_Answer4);
        Button btn_Joker = findViewById(R.id.btn_Joker);
        Button btn_Skip = findViewById(R.id.btn_Skip);


        btn_Skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(view.isActivated()) {
                    btn_Skip.setActivated(false);
                    btn_Skip.setBackgroundTintList(btn_Skip.getResources().getColorStateList(R.color.disabled_background_color));
                    btn_Skip.setTextColor(btn_Skip.getResources().getColorStateList(R.color.disabled_background_color));
                    skipScore = 0;
                    disableButtons();
                    startCooldownTimer();
                    countDownTimer.cancel();

                }
            }
        });

        btn_Joker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(view.isActivated()) {
                    btn_Joker.setActivated(false);
                    btn_Joker.setBackgroundTintList(btn_Joker.getResources().getColorStateList(R.color.disabled_background_color));
                    btn_Joker.setTextColor(btn_Joker.getResources().getColorStateList(R.color.disabled_background_color));
                    jokerScore = 0;
                    disableButtons();

                    int keepAlive;
                    do{
                        keepAlive = new Random().nextInt(4)+1;
                    }while(keepAlive == correctAnswer);

                    int reenable[] = {correctAnswer, keepAlive};
                    for(int i = 0; i <reenable.length; i++) {
                        switch (reenable[i]) {
                            case 1:
                                btn_Answer1.setEnabled(true);
                                break;
                            case 2:
                                btn_Answer2.setEnabled(true);
                                break;
                            case 3:
                                btn_Answer3.setEnabled(true);
                                break;
                            case 4:
                                btn_Answer4.setEnabled(true);
                                break;
                        }
                    }
                }
            }
        });

        btn_Answer1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(correctAnswer == 1) {
                    view.setBackgroundTintList(view.getResources().getColorStateList(R.color.green));
                    answeredCorrectly();
                }
                else {
                    view.setBackgroundTintList(view.getResources().getColorStateList(R.color.red));
                    answeredIncorrectly();
                }
            }
        });
        btn_Answer2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(correctAnswer == 2) {
                    view.setBackgroundTintList(view.getResources().getColorStateList(R.color.green));
                    answeredCorrectly();
                }
                else {
                    view.setBackgroundTintList(view.getResources().getColorStateList(R.color.red));
                    answeredIncorrectly();
                }
            }
        });
        btn_Answer3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(correctAnswer == 3) {
                    view.setBackgroundTintList(view.getResources().getColorStateList(R.color.green));
                    answeredCorrectly();
                }
                else {
                    view.setBackgroundTintList(view.getResources().getColorStateList(R.color.red));
                    answeredIncorrectly();
                }
            }
        });
        btn_Answer4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(correctAnswer == 4) {
                    view.setBackgroundTintList(view.getResources().getColorStateList(R.color.green));
                    answeredCorrectly();
                }
                else {
                    view.setBackgroundTintList(view.getResources().getColorStateList(R.color.red));
                    answeredIncorrectly();
                }
            }
        });
    }

    public void setNewQuestion(String question, String answer1, String answer2, String answer3, String answer4)
    {
        Button btn_Answer1 = findViewById(R.id.btn_Answer1);
        Button btn_Answer2 = findViewById(R.id.btn_Answer2);
        Button btn_Answer3 = findViewById(R.id.btn_Answer3);
        Button btn_Answer4 = findViewById(R.id.btn_Answer4);
        TextView lbl_Question = findViewById(R.id.lbl_Question);

        lbl_Question.setText(question);
        btn_Answer1.setText(answer1);
        btn_Answer2.setText(answer2);
        btn_Answer3.setText(answer3);
        btn_Answer4.setText(answer4);

        ImageView i = findViewById(R.id.img_Image);
        i.setImageBitmap(imgBit);

        myGoodMainChar = "";
    }

    public void answeredCorrectly() {
        countDownTimer.cancel();
        playerScore += timeLeft / 100;
        jokerScore += timeLeft / 100;
        skipScore += timeLeft / 100;
        setTitle("Score: " + Long.toString(playerScore));
        disableButtons();
        startCooldownTimer();
    }

    public void answeredIncorrectly() {
        //jokerScore = 0;
        currMistakes++;
        countDownTimer.cancel();
        showCorrectAnswer();
        disableButtons();
        startCooldownTimer();
    }

    public void enableButtons() {
        findViewById(R.id.btn_Answer1).setEnabled(true);
        findViewById(R.id.btn_Answer2).setEnabled(true);
        findViewById(R.id.btn_Answer3).setEnabled(true);
        findViewById(R.id.btn_Answer4).setEnabled(true);
    }

    public void disableButtons() {
        findViewById(R.id.btn_Answer1).setEnabled(false);
        findViewById(R.id.btn_Answer2).setEnabled(false);
        findViewById(R.id.btn_Answer3).setEnabled(false);
        findViewById(R.id.btn_Answer4).setEnabled(false);
    }

    public void displayGameover() {
        resetButtons();
        disableButtons();
        TextView lbl_Question = findViewById(R.id.lbl_Question);
        lbl_Question.setText("Game over !");
        lbl_Question.setTextSize(30.f);
    }

    public void resetButtons() {
        enableButtons();
        Button btn1 = findViewById(R.id.btn_Answer1);
        btn1.setBackgroundTintList(btn1.getResources().getColorStateList(R.color.defaultGrey));
        Button btn2 = findViewById(R.id.btn_Answer2);
        btn2.setBackgroundTintList(btn2.getResources().getColorStateList(R.color.defaultGrey));
        Button btn3 = findViewById(R.id.btn_Answer3);
        btn3.setBackgroundTintList(btn3.getResources().getColorStateList(R.color.defaultGrey));
        Button btn4 = findViewById(R.id.btn_Answer4);
        btn4.setBackgroundTintList(btn4.getResources().getColorStateList(R.color.defaultGrey));

    }

    public void showCorrectAnswer() {
        Button myBtn = null;
        switch(correctAnswer) {
        case 1:
            myBtn = findViewById(R.id.btn_Answer1);
            break;
        case 2:
            myBtn = findViewById(R.id.btn_Answer2);
            break;
        case 3:
            myBtn = findViewById(R.id.btn_Answer3);
            break;
        case 4:
            myBtn = findViewById(R.id.btn_Answer4);
            break;
        }
        if(myBtn != null)
            myBtn.setBackgroundTintList(myBtn.getResources().getColorStateList(R.color.green));
    }

    public void startLevelTimer() {
        countDownTimer = new CountDownTimer(timeToAnswer, 1000) {

            ProgressBar progressBar = findViewById(R.id.progressBar);

            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;
                TextView lbl_timeRemaining = findViewById(R.id.lbl_timeRemaining);
                lbl_timeRemaining.setText(Long.toString(millisUntilFinished / 1000));

                double remainingPercentage = ((double)timeToAnswer - millisUntilFinished) / timeToAnswer*100;
                progressBar.setProgress(100 - (int)remainingPercentage);
            }

            public void onFinish() {
                Log.d("Timer", "Timer ran out");
                progressBar.setProgress(0);
                answeredIncorrectly();
            }
        };

        countDownTimer.start();
    }

    public void startCooldownTimer() {
        roundCooldownTimer = new CountDownTimer(roundCooldown, 1000) {

            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                roundActive = false;
                gameManager();
            }
        };

        roundCooldownTimer.start();
    }

    private class GetCharacterList extends AsyncTask<String, Integer, Long> {
        Boolean firstTime = false;
        public String downloadAsString(String reqUrl) {
            StringBuilder sb = null;
            try{
                URL url = new URL(reqUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                // read the response
                InputStream in = new BufferedInputStream(conn.getInputStream());


                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                sb = new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }catch (Exception e) {e.printStackTrace();}
            return sb.toString();
        }

        public void largeLog(String tag, String content) {
            if (content.length() > 4000) {
                Log.d(tag, content.substring(0, 4000));
                largeLog(tag, content.substring(4000));
            } else {
                Log.d(tag, content);
            }
        }

        @Override
        protected Long doInBackground(String... params) {
            String response ="";
            firstTime = Boolean.parseBoolean(params[1]);
            try {
                String reqUrl = params[0];

                response = downloadAsString(reqUrl);

                JSONObject jsonObj = new JSONObject(response);
                JSONArray characters = jsonObj.getJSONArray("items");


                /*for(int i = 0; i < characters.length(); i++) {
                    JSONObject jj = characters.getJSONObject(i);
                    Log.d("item " + i, jj.toString());

                }*/

                myGoodMainChar = "";
                answers.clear();

                while(myGoodMainChar.isEmpty()) {

                    int randomChar = new Random().nextInt(characters.length());

                    JSONObject jj = characters.getJSONObject(randomChar);

                    try{
                        imgBit = BitmapFactory.decodeStream((InputStream)new URL(jj.getString("thumbnail")).getContent());
                    }catch(Exception e) {continue;}

                    String charName = jj.getString("title");
                    String charUrl = jj.getString("url");


                    String myResponse = downloadAsString(WIKIAURL + charUrl + "?action=raw");

                    myGoodTag = possibleTags.get(new Random().nextInt(possibleTags.size()));


                    String extractRes = extractTag(myGoodTag, myResponse);

                    //largeLog("extractRes", extractRes);
                    //Log.d("my Char : ", charName);


                    if(extractRes != ""  && !charsVisited.contains(charUrl)) {
                        myGoodMainChar = charName;

                        if((myGoodTag.equals("First Appearance") || myGoodTag.equals("Last Appearance")) && extractRes.charAt(1) != 'x') {
                            myResponse = downloadAsString(WIKIAURL + "/wiki/" + extractRes + "?action=raw");
                            String season = extractTag("season", myResponse);
                            String episode = extractTag("episode", myResponse);

                            Log.d("extraced episode:", season + "x" + episode);

                            String newTitle = season + "x" + episode;

                            if(newTitle.length() >= 3) {
                                answers.add(newTitle);
                            }
                            else  {
                                myGoodMainChar = "";
                                continue;
                            }

                        }
                        else {
                            answers.add(extractRes);
                        }

                        charsVisited.add(charUrl);

                    }
                }


                while(answers.size() < 4) {
                    int randomChar = new Random().nextInt(characters.length());

                    JSONObject jj = characters.getJSONObject(randomChar);

                    String charName = jj.getString("title");
                    String charUrl = jj.getString("url");

                    if(!charsVisited.contains(charUrl)) {
                        String myResponse = downloadAsString(WIKIAURL + charUrl + "?action=raw");

                        String extractRes = extractTag(myGoodTag, myResponse);

                        largeLog("extractRes", extractRes);
                        Log.d("my Char : ", charName);


                        if(extractRes != "" && extractRes != " " && extractRes != answers.get(0) && !answers.contains(extractRes)) {
                            answers.add(extractRes);
                            if((myGoodTag.equals("First Appearance") || myGoodTag.equals("Last Appearance")) && extractRes.charAt(1) != 'x') {
                                answers.remove(answers.size()-1);
                                myResponse = downloadAsString(WIKIAURL + "/wiki/" + extractRes + "?action=raw");
                                String season = extractTag("season", myResponse);
                                String episode = extractTag("episode", myResponse);

                                Log.d("extraced episode:", season + "x" + episode);

                                String newTitle = season + "x" + episode;

                                if(newTitle.length() >= 3 && !answers.contains(newTitle)) {
                                    answers.add(newTitle);
                                }
                                else continue;

                            }
                        }
                    }
                }

                Log.d("CharFinder", "My Good char is " + myGoodMainChar +  answers.toString() + "had to visit: " + charsVisited.toString());
                Log.d("All answers", answers.toString());

            }catch(Exception e) {
                largeLog("log", e.toString());
                e.printStackTrace();
                questionGeneratorReturn = -1;
            }

            return 1L;
        }


        public String extractTag(String tag, String input) {
            String s;
            s = input.substring(input.indexOf("|" + tag) + 1);
            s = s.substring(0, s.indexOf("\n"));


            Log.d("test1", s);

            //check if found actual result
            if(!input.substring(0, 5).equals(s.substring(0, 5)) && s.length() > tag.length() + " = ".length()) {
                s = s.substring(tag.length() + " = ".length());
                if(s.charAt(0) == '[') {
                    if(s.indexOf('|') != -1) {
                        s = s.substring(s.lastIndexOf('|') + 1);
                    }
                    else s = s.substring(s.lastIndexOf('[') + 1);
                    s = s.substring(0, s.indexOf(']'));
                }
                else if(s.charAt(0) == '{') {
                    if(s.indexOf('|') != -1) {
                        s = s.substring(s.lastIndexOf('|') + 1);
                    }
                    else s = s.substring(s.lastIndexOf('{') + 1);
                    s = s.substring(0, s.indexOf('}'));
                }
                else {
                    ArrayList<Integer> positions = new ArrayList<Integer>();
                    positions.add(s.indexOf("<"));
                    positions.add(s.indexOf("{"));
                    positions.add(s.indexOf("|"));

                    int smallest = Integer.MAX_VALUE;
                    if(positions.size() >= 1) {
                        for(int i = 0; i < positions.size(); i++) {
                            if(positions.get(i) < smallest && positions.get(i) != -1) {
                                smallest = positions.get(i);
                            }
                        }
                    }
                    if(smallest != Integer.MAX_VALUE)
                      s = s.substring(0, smallest);
                }

            }
            else {
                return "";
            }
            Log.d("test2", s);

            String myReturn = "";

            if(s.length() > 1) {
                myReturn = s;
                if(myReturn.charAt(myReturn.length()-1) == ' ') {
                    myReturn = myReturn.substring(0, myReturn.length()-1);
                }
            }
            else if (s.length() == 1 && (s.charAt(0) >= '0' && s.charAt(0) <= '9'))
                myReturn = s;

            Log.d("myReturn", myReturn);

            myReturn = myReturn.replace("\"", "");
            myReturn = myReturn.replace("{", "");
            myReturn = myReturn.replace("}", "");
            myReturn = myReturn.replace("[", "");
            myReturn = myReturn.replace("]", "");


            return myReturn;
        }

        public String constructQuestion() {
            String question = "";
            if(myGoodTag.equals("Aliases") || myGoodTag.equals("AKA")) {
                question = "What is an alias of " + myGoodMainChar + " ?";
            }
            else if(myGoodTag.equals("Age") || myGoodTag.equals("age")) {
                question = "What is the age of " + myGoodMainChar + " ?";
            }
            else if(myGoodTag.equals("Portrayed by") || myGoodTag.equals("actor")) {
                question = "By whom is " + myGoodMainChar + " portrayed?";
            }
            else if(myGoodTag.equals("First Appearance")) {
                question = "What is the first appearance of " + myGoodMainChar + " ?";
            }
            else if(myGoodTag.equals("Last Appearance")) {
                question = "What is the last appearance of " + myGoodMainChar + " ?";
            }
            else if(myGoodTag.equals("Status")) {
                question = "What is the status of " + myGoodMainChar + " ?";
            }
            return question;
        }

        protected void onPostExecute(Long result) {
            try{
                String myCorrectAnswer = answers.get(0);

                ArrayList<String> randList = (ArrayList<String>) answers.clone();
                Collections.shuffle(randList);

                answers = randList;

                nextCorrectAnswer = randList.indexOf(myCorrectAnswer)+1;

                Log.d("Correct answer is", Integer.toString(randList.indexOf(myCorrectAnswer)));

                constructedQuestion = constructQuestion();

                if(firstTime) {
                    correctAnswer = nextCorrectAnswer;
                    setNewQuestion(constructedQuestion, randList.get(0),randList.get(1),randList.get(2),randList.get(3));
                    startLevelTimer();
                    new GetCharacterList().execute(WIKIAURL + "/api/v1/Articles/Top?expand=1&category=Characters&limit=" + CRAWLAMOUNT, "false");

                }
            }catch(Exception e) {
                new GetCharacterList().execute(WIKIAURL + "/api/v1/Articles/Top?expand=1&category=Characters&limit=" + CRAWLAMOUNT, "false");
            }
        }

    }
}
