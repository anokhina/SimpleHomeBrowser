/*
 * Copyright 2017 Veronica Anokhina.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.org.sevn.simplehomebrowser;

import com.sun.javafx.webkit.Accessor;
import com.sun.webkit.LoadListenerClient;
import com.sun.webkit.WebPage;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
 
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Worker;
 
import static javafx.concurrent.Worker.State.FAILED;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.PromptData;
import javafx.scene.web.WebErrorEvent;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javax.imageio.ImageIO;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.json.JSONArray;
import org.json.JSONObject;

import netscape.javascript.JSObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.html.HTMLImageElement;

                        /*
                        http://docs.oracle.com/javase/8/javafx/api/javafx/scene/web/WebEngine.html
                        EventListener listener = new EventListener() {
                            public void handleEvent(Event ev) {
                                Platform.exit();
                            }
                        };                        
                        ((EventTarget) el).addEventListener("click", listener, false);
                        webEngine.executeScript("history.back()");
http://stackoverflow.com/questions/14385233/setting-a-cookie-using-javafxs-webengine-webview
WebView webView = new WebView();
URI uri = URI.create("http://mysite.com");
Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();
headers.put("Set-Cookie", Arrays.asList("name=value"));
java.net.CookieHandler.getDefault().put(uri, headers);
webView.getEngine().load("http://mysite.com");

java.net.CookieManager manager = new java.net.CookieManager();
java.net.CookieHandler.setDefault(manager);
manager.getCookieStore().removeAll();
or
java.net.CookieHandler.setDefault(new java.net.CookieManager());
                        */

//http://docs.oracle.com/javase/8/javafx/api/javafx/scene/web/WebEngine.html
public class SimpleHomeBrowser extends JFrame {
 
    private final JFXPanel jfxPanel = new JFXPanel();
    private final JFXPanel jfxPanel1 = new JFXPanel();
    private final ImageView imageView = new ImageView();
    
    private WebEngine engine;
 
    private JPanel topBar;
    private JPanel statusBar;
    private final JPanel panel = new JPanel(new BorderLayout());
    private final JLabel lblStatus = new JLabel();
 
    private final JButton btnGo = new JButton("Go!");
    private final JButton btnHome = new JButton("Home"+SHK_HOME);
    private final JButton btnHistory = new JButton("History");
    private final JButton btnRunJsFromControl = new JButton("runIt()");
    private final JButton btnX = new JButton("X"+SHK_X);
    private final JTextField txtURL = new JTextField();
    private final JProgressBar progressBar = new JProgressBar();
 
    private Path logPath;
    private Path historyPath;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private History history = new History();
    
    public SimpleHomeBrowser() {
        super();
        logPath = getLogPath();
        historyPath = getHistoryPath();
        java.io.File hist = getHistoryFile();
        if (hist.exists()) {
            try {
                JSONArray jarr = new JSONArray(new String(Files.readAllBytes(historyPath), "UTF-8"));
                history.fromJSON(jarr);
            } catch (IOException ex) {
                Logger.getLogger(SimpleHomeBrowser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        setUndecorated(true);
        addKeyBindings();
        initComponents();
    }
    
    protected Path getLogPath() {
        return new java.io.File(System.getProperty("user.home") + "/sevn-simple-browser-log.txt").toPath();
    }
    protected Path getHistoryPath() {
        return getHistoryFile().toPath();
    }
    protected java.io.File getHistoryFile() {
        return new java.io.File(System.getProperty("user.home") + "/sevn-simple-browser.txt");
    }
 
    private final Action openFromHistory = new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent e) {
            //TODO 
            ArrayList possibilitiesLst = new ArrayList(history.values());
            Collections.reverse(possibilitiesLst);
            Object[] possibilities = possibilitiesLst.toArray();
            Object def = null;
            if (possibilities.length > 0) {
                def = possibilities[0];
            }
            UrlInfo s = (UrlInfo) JOptionPane.showInputDialog(SimpleHomeBrowser.this,
                    "Open url:\n",
                    "History",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    possibilities,
                    def);
            if (s != null) {
                loadURL(s.getUrl());
            }
        }
    };
    private final Action exitAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {                
                        JFrame frame = SimpleHomeBrowser.this;
                        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                    }});
            }
        };
    private final Action homeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String curl = lastUrl;
                String url = null;
                if (curl != null) {
                    url = getEmbededUrl(curl);
                }
                if (url != null) {
                    loadURL(url);
                } else {
                    if (curl != null && curl.startsWith(EMB_STR)) {
                        String v = curl.substring(EMB_STR.length());
                        String u = "https://www.youtube.com/watch?v=" + v;
                        loadURL(u);
                    } else {
                        loadURL("http://youtube.com");
                    }
                }
            }
    };
    private void initComponents() {
        createScene();
 
        ActionListener al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadURL(txtURL.getText());
            }
        };
 
        btnGo.addActionListener(al);
        btnX.addActionListener(exitAction);
        btnHome.addActionListener(homeAction);
        btnHistory.addActionListener(openFromHistory);
        btnRunJsFromControl.addActionListener((e) -> {
            System.out.println("RunJsFromControl");
            runJsFromControl();
        });
        txtURL.addActionListener(al);
 
        progressBar.setPreferredSize(new Dimension(150, 18));
        progressBar.setStringPainted(true);
 
        topBar = new JPanel(new BorderLayout(5, 0));
        topBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        topBar.add(txtURL, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        topBar.add(buttons, BorderLayout.EAST);
        buttons.add(btnGo);
        buttons.add(btnHistory);
        buttons.add(btnRunJsFromControl);
        buttons.add(btnHome);
        buttons.add(btnX);
 
        statusBar = new JPanel(new BorderLayout(5, 0));
        statusBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        statusBar.add(lblStatus, BorderLayout.CENTER);
        statusBar.add(progressBar, BorderLayout.EAST);
 
        panel.add(topBar, BorderLayout.NORTH);
        panel.add(jfxPanel, BorderLayout.CENTER);
        panel.add(jfxPanel1, BorderLayout.WEST);
        panel.add(statusBar, BorderLayout.SOUTH);
 
        getContentPane().add(panel);
 
        setPreferredSize(new Dimension(1024, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
    }
    
    @Override
    public void setTitle(String title) { 
        super.setTitle(title);
        String key = getKey(lastUrl);
        UrlInfo ui = history.get(key);
        if (ui != null && title != null && title.trim().length() > 0) {
            ui.setTitle(title);
        }
        updateStatus();
    }
    
    private void setLastUrl(String u) {
        if (u != null) {
            lastUrl = u;
            String key = getKey(lastUrl);
            UrlInfo ui = history.get(key);
            if (ui == null) {
                ui = new UrlInfo().setUrl(u);
                if (!u.equals(key)) {
                    ui.setVideo(key);
                }
            }
            lastUrlInfo = ui;
            history.put(key, ui);
            saveHistory();
            updateStatus();
            if (logPath != null) {
                String msg = sdf.format(new Date()) + ": " + lastUrlInfo.toFullString() + "\n";
                try {
                    Files.write(logPath, msg.getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(SimpleHomeBrowser.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(SimpleHomeBrowser.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    private void updateStatus() {
        String tt = "";
        if (lastUrlInfo != null) {
            tt = lastUrlInfo.toFullString();
        }
        lblStatus.setToolTipText(tt);
    }
 
    private UrlInfo lastUrlInfo;
    private String lastUrl;
    private WebView webView;
    private void createScene() {
 
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
 
                webView = new WebView();
                engine = webView.getEngine();
                engine.setJavaScriptEnabled(true);
                //com.sun.webkit.LoadListenerClient
                final WebPage page = Accessor.getPageFor(engine);
                page.addLoadListenerClient(new LoadListenerClient() {
                    //see LoadListenerClient.PAGE_REDIRECTED
                    @Override
                    public void dispatchLoadEvent(long frame, int state, String url, String contentType, double progress, int errorCode) {
                        JSONObject jobj = new JSONObject().put("mainFrame", page.getMainFrame()).put("frame", frame).put("url", url).put("state", state).put("contentType", contentType).put("progress", progress).put("errorCode", errorCode);
                        if (page.getMainFrame() == frame) {
                            if (!url.equals(lastUrl)) {
                                setLastUrl(url);
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        txtURL.setText(url);
                                    }
                                });                                
                            }
                        } else {
                            //System.err.println(jobj.toString(2));
                        }
                    }

                    @Override
                    public void dispatchResourceLoadEvent(long frame, int state, String url, String contentType, double progress, int errorCode) {
//                        JSONObject jobj = new JSONObject().put("RES", "RES").put("frame", frame).put("url", url).put("state", state).put("contentType", contentType).put("progress", progress).put("errorCode", errorCode);
//                        System.err.println(jobj.toString(2));
                    }
                });
 
                webView.setContextMenuEnabled(true);
                createContextMenu(webView);
                webView.getEngine().setOnError(new EventHandler<WebErrorEvent>() {
                    @Override
                    public void handle(WebErrorEvent event) {
                        System.err.println(""+event.getMessage());
                    }
                });
                webView.getEngine().setOnAlert(new EventHandler<WebEvent<String>>() {
                    @Override
                    public void handle(WebEvent<String> arg0) {
                        JOptionPane.showMessageDialog(null, arg0.getData(), "Message", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
                webView.getEngine().setConfirmHandler(new Callback<String, Boolean>() {
                    public Boolean call(String msg) {
                        int result = JOptionPane.showConfirmDialog(null, msg, "Message", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                        boolean b = (result != 0);
                        return !b;
                    }
                });
                webView.getEngine().setPromptHandler(new Callback<PromptData, String>() {
                    @Override
                    public String call(PromptData param) {
                        return (String)JOptionPane.showInputDialog(null, param.getMessage(), "Message", JOptionPane.INFORMATION_MESSAGE, null, null, param.getDefaultValue());
                    }
                });
                engine.titleProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, final String newValue) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                SimpleHomeBrowser.this.setTitle(newValue);
//                                txtURL.setText(lastUrl);
                            }
                        });
                    }
                });
 
                engine.setOnStatusChanged(new EventHandler<WebEvent<String>>() {
                    @Override
                    public void handle(final WebEvent<String> event) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                lblStatus.setText(event.getData());
                            }
                        });
                    }
                });
 
//                engine.locationProperty().addListener(new ChangeListener<String>() {
//                    @Override
//                    public void changed(ObservableValue<? extends String> ov, String oldValue, final String newValue) {
//                        SwingUtilities.invokeLater(new Runnable() {
//                            @Override
//                            public void run() {
//                                txtURL.setText(newValue);
//                            }
//                        });
//                    }
//                });
 
                engine.getLoadWorker().workDoneProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, final Number newValue) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setValue(newValue.intValue());
                            }
                        });
                    }
                });
 
                engine.getLoadWorker()
                        .exceptionProperty()
                        .addListener(new ChangeListener<Throwable>() {
 
                            @Override
                            public void changed(ObservableValue<? extends Throwable> o, Throwable old, final Throwable value) {
                                if (engine.getLoadWorker().getState() == FAILED) {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            JOptionPane.showMessageDialog(
                                            panel,
                                            (value != null)
                                            ? engine.getLocation() + "\n" + value.getMessage()
                                            : engine.getLocation() + "\nUnexpected error.",
                                            "Loading error...",
                                            JOptionPane.ERROR_MESSAGE);
                                        }
                                    });
                                }
                            }
                        });
 
                engine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                        System.out.println("-----"+newValue);
                    if (Worker.State.SUCCEEDED.equals(newValue)) {
                        JSObject jsobj = (JSObject) engine.executeScript("window");
                        jsobj.setMember("JavaFileLoader", new JavaFileLoader());
                        engine.executeScript("JavaFileLoader.logIt('It works')");
                        //captureView(jfxPanel);
// engine.getLocation() DOESNT WORK PROPERLY !!!
//                        SwingUtilities.invokeLater(new Runnable() {
//                            @Override
//                            public void run() {
//                                txtURL.setText(engine.getLocation());
//                            }
//                        });
                    }
                });                
                jfxPanel.setScene(new Scene(webView));
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                imageView.setCache(true);                
                BorderPane borderpane = new BorderPane();
                borderpane.setCenter(imageView);                
                jfxPanel1.setScene(new Scene(borderpane));
            }
        });
    }
    
    public static Map<String, List<String>> getQueryParams(String q) throws UnsupportedEncodingException {
        final Map<String, List<String>> ret = new LinkedHashMap();
        if (q != null) {
            for (String pair : q.split("&")) {
                int idx = pair.indexOf("=");
                String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
                List<String> entry = ret.get(key);
                if (entry == null) {
                    entry = new LinkedList<String>();
                    ret.put(key, entry);
                }
                String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
                entry.add(value);
            }
        }
        return ret;
    }  
    
    public static final String EMB_STR = "https://www.youtube.com/embed/";
    
    private String getEmbededUrl(String lastUrl) {
        String v = getEmbededVideoId(lastUrl);
        if (v != null) {
            return EMB_STR + v;
        }
        return null;
    }
    private String getEmbededVideoId(String lastUrl) {
        String v = null;
        URI uri = null;
        try {
            uri = new URI(lastUrl);
            List<String> vs = getQueryParams(uri.getQuery()).get("v");
            if (vs != null) {
                v = vs.get(0);
            }
        } catch (URISyntaxException | UnsupportedEncodingException ex) {
            Logger.getLogger(SimpleHomeBrowser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return v;
    }
 
    public void loadURL(final String url) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                String tmp = toURL(url);
 
                if (tmp == null) {
                    tmp = toURL("http://" + url);
                }
 
                setLastUrl(tmp);
                engine.load(tmp);
            }
        });
    }
 
    private static String toURL(String str) {
        try {
            return new URL(str).toExternalForm();
        } catch (MalformedURLException exception) {
            return null;
        }
    }
 
    public static Rectangle getWorkBounds(Window wnd) {
        Rectangle rect = wnd.getGraphicsConfiguration().getBounds();
        java.awt.Insets ins = wnd.getToolkit().getScreenInsets(wnd.getGraphicsConfiguration());
        return getWorkBounds(rect, ins);
    }
    public static Rectangle getWorkBounds(Rectangle rect, java.awt.Insets ins) {
        rect.x += ins.left;
        rect.y += ins.top;
        rect.height -= ins.bottom;
        rect.width -= ins.right;
        return rect;
    }    
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
 
            @Override
            public void run() {
                SimpleHomeBrowser browser = new SimpleHomeBrowser();
                browser.setVisible(true);
                browser.setBounds(getWorkBounds(browser));
                browser.openLast();
            }
        });
    }
    
    public void openLast() {
        UrlInfo ui = history.getTail();
        String url = "http://youtube.com";
        if (ui != null && ui.getUrl() != null) {
            if (ui.getVideo() != null && ui.getVideo().trim().length() > 0) {
                url = EMB_STR + ui.getVideo();
            } else {
                url = ui.getUrl();
            }
        }
        loadURL(url);
    }
    
    public static final String SHK_HOME = "(^-W)";
    public static final String SHK_X = "(^-Q)";
    private void addKeyBindings() {
        JFrame frame = this;
        //"alt shift released X" =&gt; getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK, true);
        // KeyEvent.VK_SPACE, KeyEvent.VK_N, KeyEvent.VK_P, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_ESCAPE, KeyEvent.VK_F, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_HOME, KeyEvent.VK_END
        for(int i: new int[] {KeyEvent.VK_F, KeyEvent.VK_ESCAPE}) {
            new ActionName(i, "SEVN"+i).bind(frame);
        }
        
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK, true), SHK_HOME);
        frame.getRootPane().getActionMap().put(SHK_HOME, homeAction);
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK, true), SHK_X);
        frame.getRootPane().getActionMap().put(SHK_X, exitAction);
    }

    private void saveHistory() {
        if (historyPath != null) {
            try {
                Files.write(historyPath, history.toJSON().toString(2).getBytes("UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(SimpleHomeBrowser.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(SimpleHomeBrowser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    class ActionName {
        private final int keyCode;
        private final String name;
        public ActionName(int kc, String n) {
            this.keyCode = kc;
            this.name = n;
        }
        public int getKeyCode() {
            return keyCode;
        }
        public String getName() {
            return name;
        }
        public KeyStroke getKeyStroke() {
            return KeyStroke.getKeyStroke(getKeyCode(), 0);
        }
        public void bind(JFrame frame) {
            frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(getKeyStroke(), getName());
            frame.getRootPane().getActionMap().put(getName(), new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    keyPressed(ActionName.this, e);
                }
            });
        }
        public void keyPressed(ActionName e, ActionEvent evt) {
        	if(e.getKeyCode()==KeyEvent.VK_F){
                toggleFullScreen();
            } else if (e.getKeyCode()==KeyEvent.VK_F) {
                if (fscreen) {
                    toggleFullScreen();
                }
            }
        }
    }
    
    private boolean fscreen;
    
    private void toggleFullScreen() {
        if (fscreen) {
            fscreen = !true;
            setExtendedState(JFrame.NORMAL);
            webView.requestFocus();
        } else {
            fscreen = true;
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
        topBar.setVisible(!fscreen);
        statusBar.setVisible(!fscreen);
    }
    
    public static class UrlInfo {
        private String url;
        private String title;
        private int weight;
        private int watches;
        private String video;
        
        public JSONObject toJSON() {
            return new JSONObject(toJsonMap());
        }
        
        public UrlInfo fromJSON(JSONObject jobj) {
            if (jobj.has("url")) url = jobj.getString("url");
            if (jobj.has("title")) title = jobj.getString("title");
            if (jobj.has("video")) video = jobj.getString("video");
            if (jobj.has("weight")) weight = jobj.getInt("weight");
            if (jobj.has("watches")) watches = jobj.getInt("watches");
            return this;
        }
        
        public Map<String, Object> toJsonMap() {
            LinkedHashMap<String, Object> ret = new LinkedHashMap<>();
            if (url != null) ret.put("url", url);
            if (title != null) ret.put("title", title);
            ret.put("weight", weight);
            ret.put("watches", watches);
            if (video != null) ret.put("video", video);
            return ret;
        }
        
        private String nn(String s) {
            return (s == null) ? "" : s;
        }
        @Override
        public String toString() {
            return toShortString();
        }
        public String toShortString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(nn(video)).append("] ");
            if (title != null)  {
                sb.append(title);
            } else {
                sb.append(url);
            }
            return sb.toString();
        }
        
        public String toFullString() {
            StringBuilder sb = new StringBuilder();
            sb.append(watches).append(": ").append(weight).append(": ").append(nn(video)).append(": ").append(nn(title)).append(": ").append(nn(url));
            return sb.toString();
        }

        public String getUrl() {
            return url;
        }

        public UrlInfo setUrl(String url) {
            this.url = url;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public UrlInfo setTitle(String title) {
            this.title = title;
            return this;
        }

        public int getWeight() {
            return weight;
        }

        public UrlInfo setWeight(int weight) {
            this.weight = weight;
            return this;
        }
        
        public int incr(int i) {
            return (weight += i);
        }

        public String getVideo() {
            return video;
        }

        public UrlInfo setVideo(String video) {
            this.video = video;
            return this;
        }

        public int getWatches() {
            return watches;
        }

        public void setWatches(int watches) {
            this.watches = watches;
        }
        
    }
    public static class History extends LinkedHashMap<String, UrlInfo> {
        private String tail;
        public UrlInfo getTail() {
            UrlInfo ret = get(tail);
            if (ret == null && size() > 0) {
                tail = null;
                for (String k : keySet()) {
                    tail = k;
                }
                ret = get(tail);
            }
            return ret;
        }
        private UrlInfo superput(String k, UrlInfo ui) {
            tail = k;
            return super.put(k, ui);
        }
        public UrlInfo put(String k, UrlInfo ui) {
            if (containsKey(k)) {
                remove(k);
            }
            ui.weight++;
            UrlInfo ret = superput(k, ui);
            if (size() > 150) {
                String kn = null;
                for (String n : keySet()) {
                    kn = n;
                    break;
                }
                remove(kn);
            }
            return ret;
        }
        public JSONArray toJSON() {
            JSONArray ret = new JSONArray();
            for(UrlInfo ui : values()) {
                ret.put(ui.toJSON());
            }
            return ret;
        }        
        public void fromJSON(JSONArray jobj) {
            for(Object o : jobj) {
                JSONObject jo = (JSONObject)o;
                UrlInfo ui = new UrlInfo().fromJSON(jo);
                String k = ui.getVideo();
                if (k == null) {
                    k = ui.getUrl();
                }
                superput(k, ui);
                if (size() > 150) break;
            }
        }        
    }
    
    private String getKey(String u) {
        if (u != null) {
            if (u.startsWith(EMB_STR)) {
                return u.substring(EMB_STR.length());
            }
            String ret = getEmbededVideoId(u);
            if (ret != null) {
                return ret;
            }
        }
        return u;
    }

    private static final File outDir = new File("D:\\USERTEMP\\jfxout");
    
    private static String makeFileName(String ext) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm_ssSSS_Z");
        return "demo"+sdf.format(new Date())+ext;
    }
    
    public static String toDocumentString(Document doc) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            javax.xml.transform.Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }
    
    private void runJsFromControl() {
        Platform.runLater(()->{
            engine.executeScript("main()");
        });
    }
    private void captureImage(String imageId, String fileName, String format) {
        Element el = engine.getDocument().getElementById(imageId);
        //Object el1 = engine.executeScript("document.getElementById('"+imageId+"')");
        System.out.println("captureImage>"+el);
        if (el instanceof HTMLImageElement) {
            HTMLImageElement img = (HTMLImageElement)el;
            System.out.println("img_src>"+img.getSrc());
            javafx.scene.image.Image image = new javafx.scene.image.Image(img.getSrc());
            imageView.setImage(image);
            captureNode(imageView, fileName, format);
        }
        //com.sun.webkit.dom.HTMLImageElementImpl img = (com.sun.webkit.dom.HTMLImageElementImpl)el;
    }
    private void captureNode(Node node, String fileName, String format) {
        if (!outDir.exists()) outDir.mkdirs();
        
        WritableImage image = node.snapshot(new SnapshotParameters(), null);
        BufferedImage bi = SwingFXUtils.fromFXImage(image, null);        
        bi.flush();
        try {
            if (fileName == null) {
                fileName = makeFileName(".png");
            }
            if (format == null) {
                format = "PNG";
            }
            ImageIO.write(bi, format, new File(outDir, fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    private static void captureView(JFXPanel jfxPanel) {
        if (!outDir.exists()) outDir.mkdirs();
        
        BufferedImage bi = new BufferedImage(jfxPanel.getWidth(), jfxPanel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = bi.createGraphics();
        jfxPanel.paint(graphics);
        bi.flush();
        try {
            ImageIO.write(bi, "PNG", new File(outDir, makeFileName(".png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        graphics.dispose();
    }

    public class JavaFileLoader {

        public int captureImage(String imageId, String fileName, String format) {
            SimpleHomeBrowser.this.captureImage(imageId, fileName, format);
            return 0;
        }
        public int logIt(Object o) {
            System.out.println(""+o);
            return 0;
        }
    }
    
    private void createContextMenu(WebView webView) {
//        ContextMenu contextMenu = new ContextMenu();
//        MenuItem reload = new MenuItem("Reload");
//        reload.setOnAction(e -> webView.getEngine().reload());
//        MenuItem savePage = new MenuItem("Save Page");
//        savePage.setOnAction(e -> System.out.println("Save page..."));
//        MenuItem hideImages = new MenuItem("Hide Images");
//        hideImages.setOnAction(e -> System.out.println("Hide Images..."));
//        contextMenu.getItems().addAll(reload, savePage, hideImages);

//        webView.setOnMousePressed(e -> {
//            if (e.getButton() == MouseButton.MIDDLE.SECONDARY) {
//                contextMenu.show(webView, e.getScreenX(), e.getScreenY());
//            } else {
//                contextMenu.hide();
//            }
//        });
    }    

    private void saveFile(javafx.stage.Window win, Node node){
        if (node == null) return;
        SnapshotParameters sp = new SnapshotParameters();
        //sp.setFill(Color.TRANSPARENT);
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        fc.setTitle("Save into file");
        File file = fc.showSaveDialog(win);
        if (file != null) {
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(node.snapshot(sp, null), null), "png", file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


//TODO focus to webview