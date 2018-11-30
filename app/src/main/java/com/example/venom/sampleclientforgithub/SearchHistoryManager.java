package com.example.venom.sampleclientforgithub;

import android.content.Context;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SearchHistoryManager {
    private static Context context;

    private Set<String> searchList;
    private String filename ;
    private ObjectMapper mapper = new ObjectMapper();

    public SearchHistoryManager(Context c) {
        this.context = c;
        filename = String.valueOf(context.getResources().getText(R.string.history_filename));
        loadData();
    }

    private void loadData() {
        try {
            if (!fileExists(filename)) {
                resetHistory();
            }
            searchList = mapper.readValue(context.openFileInput(filename), Set.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(searchList);
    }

    public void resetHistory() {
        List<String> list = new ArrayList<String>();
        list.clear();
        try {
            mapper.writeValue(context.openFileOutput(filename, Context.MODE_WORLD_WRITEABLE), list);
            if (searchList != null)
                searchList.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addSearch(String s) {
        try {
            searchList.add(s);
            mapper.writeValue(context.openFileOutput(filename, Context.MODE_WORLD_WRITEABLE), searchList);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<String> getSearches() {
        return searchList;
    }

    private boolean fileExists(String fname) {
        File file = context.getFileStreamPath(fname);
        return file.exists();
    }
}
