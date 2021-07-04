package com.hellrider.redtest;

import java.io.Serializable;

public class DataItem implements Serializable {

    private String title, body;

    DataItem(String title, String body)
    {
        setTitle(title);
        setBody(body);
    }

    public String getTitle()
    {
        return title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

