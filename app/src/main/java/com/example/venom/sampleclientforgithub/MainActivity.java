package com.example.venom.sampleclientforgithub;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.egit.github.core.SearchRepository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private int MAX_CONCURRENT_REPOS;
    private String gitHubApiToken;

    private Menu menu;
    private RepositoryService service;
    private Intent intent;
    private boolean hasResults = false;
    private ListView listView;
    private Button showButton;
    public ArrayList<String> list, selectedItems;
    SearchHistoryManager history;

    /**
     * Android activity creation handler
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.err.println("onCREATE callback");

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        SharedPreferences sharedPref = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        gitHubApiToken = sharedPref.getString("gitHubApiToken", "");

        if (gitHubApiToken.isEmpty()) {
            startLoginActivity();
        }

        history = new SearchHistoryManager(this);
        MAX_CONCURRENT_REPOS = getResources().getInteger(R.integer.concurrent_repositories);

        showButton = (Button) findViewById(R.id.showButton);
        listView = (ListView) findViewById(R.id.mainListView);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        hasResults = history.getSearches().size() > 0 ? true : false;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArrayAdapter<String> listAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.single_repo, new ArrayList(history.getSearches()));
                listView.setAdapter(listAdapter);
            }
        });

        listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

                hideKeyboard();

                intent = new Intent(MainActivity.this, CommitsActivity.class);
                if (hasResults) {
                    selectedItems = new ArrayList<String>();
                    SparseBooleanArray checked = ((ListView) parent).getCheckedItemPositions();
                    for (int i = 0; i < ((ListView) parent).getAdapter().getCount(); i++) {
                        if (checked.get(i)) {
                            selectedItems.add(((ListView) parent).getItemAtPosition(i).toString());
                        }
                    }

                    intent.putStringArrayListExtra("reposId", selectedItems);

                    int numberSelectedRepos = selectedItems.size();

                    if (numberSelectedRepos > 0 && numberSelectedRepos <= MAX_CONCURRENT_REPOS) {
                        showButton.setClickable(true);
                        showButton.setText("Show commits");
                        showButton.setBackgroundResource(R.drawable.roundedbutton);
                        showButton.setVisibility(View.VISIBLE);
                    } else if (numberSelectedRepos > MAX_CONCURRENT_REPOS) {
                        showButton.setText("Too many repos");
                        showButton.setClickable(false);
                        showButton.setBackgroundResource(R.drawable.roundedbutton_disabled);
                        showButton.setVisibility(View.VISIBLE);

                    } else {
                        showButton.setClickable(false);
                        showButton.setVisibility(View.GONE);
                    }
                }
            }
        });


        EditText edit_txt = (EditText) findViewById(R.id.searchField);
        edit_txt.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    searchAndDisplay(null);
                    return true;
                }
                return false;
            }
        });


        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(gitHubApiToken);
        service = new RepositoryService(client);

        list = new ArrayList<>();
    }

    public void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void signIn(MenuItem v) {
        SharedPreferences settings = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        settings.edit().clear().commit();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("SIGN_IN", true);
        startActivity(intent);
    }

    public void signOut(MenuItem v) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                clearHistory(null);
                Intent inte = new Intent(MainActivity.this, LoginActivity.class);
                inte.putExtra("SIGN_OUT", true);
                startActivity(inte);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        System.err.println("onPrepareOptionsMenu callback");

        if (gitHubApiToken.equals(getResources().getString(R.string.github_public_token))) {
            MenuItem signInButton = menu.findItem(R.id.action_signin);
            signInButton.setVisible(true);

            MenuItem signOutButton = menu.findItem(R.id.action_signout);
            signOutButton.setVisible(false);
        }
        else {
            MenuItem signInButton = menu.findItem(R.id.action_signin);
            signInButton.setVisible(false);

            MenuItem signOutButton = menu.findItem(R.id.action_signout);
            signOutButton.setVisible(true);
        }
        return true;
    }

    public void clearField(final View view) {
        EditText editText = (EditText) findViewById(R.id.searchField);
        editText.setText("");
    }

    public void startCommitActivity(View v) {
        System.out.println(selectedItems);
        for (String item : selectedItems) {
            history.addSearch(item);
        }
        startActivity(intent);
    }


    public void clearHistory(MenuItem v) {
        history.resetHistory();
        // Display historical searches
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArrayAdapter<String> listAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.single_repo, new ArrayList(history.getSearches()));
                listView.setAdapter(listAdapter);
            }
        });
    }

    public void searchAndDisplay(final View v) {
        hideKeyboard();

        String searchSentence = ((EditText) findViewById(R.id.searchField)).getText().toString();
        if (searchSentence.isEmpty())
            return;

        Log.i("info", "SEARCH '" + searchSentence + "'");

        try {
            List<SearchRepository> listRepo = service.searchRepositories(searchSentence);

            this.list.clear();
            for (SearchRepository repository : listRepo) {
                String repo = repository.isPrivate() ? "\uD83D\uDD12" + repository.getId() : repository.getId();

                this.list.add(repo);
            }
            hasResults = !this.list.isEmpty();

            if (!hasResults) this.list.add("No result for '" + searchSentence + "'");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ArrayAdapter<String> listAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.single_repo, MainActivity.this.list);
                    listView.setAdapter(listAdapter);
                }
            });

            int numResults = listRepo.size();
            String message = numResults > 99 ? "> 100" : numResults == 0 ? "No" : numResults + "";
            message += " repositories found";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        } catch (UnknownHostException e) {
            Log.i("info", "Connection fail, check the phone is connected to network");

            Toast.makeText(this, "You need to connect to the Internet", Toast.LENGTH_SHORT).show();

        } catch (RequestException e) {
            signIn(null);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void hideKeyboard() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Hide the virtual keyboard
                if (MainActivity.this.getCurrentFocus() != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(MainActivity.this.getCurrentFocus().getWindowToken(), 0);
                }
                showButton.setVisibility(View.GONE);
            }
        });
    }


}
