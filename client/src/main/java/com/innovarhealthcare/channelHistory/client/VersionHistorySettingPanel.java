/*
 * This Source Code Form is subject to the terms of the
 * Mozilla Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.innovarhealthcare.channelHistory.client;

import com.innovarhealthcare.channelHistory.shared.VersionControlConstants;
import com.innovarhealthcare.channelHistory.shared.interfaces.channelHistoryServletInterface;
import com.mirth.connect.client.ui.AbstractSettingsPanel;
import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.components.*;
import lombok.SneakyThrows;
import net.miginfocom.swing.MigLayout;
import org.json.JSONObject;


import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Jim(Zi Min) Weng
 * @create 2024-04-19 10:00 AM
 */
public class VersionHistorySettingPanel extends AbstractSettingsPanel {

    private MirthCheckBox enableGitCheckbox;
    private VersionHistorySettingPlugin plugin;
    private MirthTextField remoteRepoUrl;
    private MirthTextField remoteBranch;
    private MirthTextArea remoteSshKey;
    private MirthButton uploadSshKeyButton;
    private MirthButton validateSettingButton;
    private JScrollPane remoteSshKeyScrollPane;
    private channelHistoryServletInterface gitServlet;
    private Frame parent;

    public VersionHistorySettingPanel(String tabName, VersionHistorySettingPlugin plugin) throws Exception {
        super(tabName);
        this.plugin = plugin;
        this.parent = PlatformUI.MIRTH_FRAME;
        initComponents();
    }
    private void initComponents() throws Exception{
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setLayout(new MigLayout("insets 12, novisualpadding, hidemode 3, fill, gap 6", "", "[][][][grow]"));


        JPanel vHistoryPanel = new JPanel();
        vHistoryPanel.setLayout(new MigLayout("insets 12, novisualpadding, hidemode 3, fill, gap 6", "[]12[][grow]", ""));

        vHistoryPanel.setBackground(Color.WHITE);
        vHistoryPanel.setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 0, new Color(204, 204, 204)),
                        "Innover Version History Plugin",
                        TitledBorder.DEFAULT_JUSTIFICATION,
                        1,
                        new Font("Tahoma", 1, 11)
                )
        );

        enableGitCheckbox = new MirthCheckBox("Enable Version Control");
        enableGitCheckbox.setBackground(Color.WHITE);
        enableGitCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean enableGit = enableGitCheckbox.isSelected();
                enableFields(enableGit);
            }
        });

        JLabel remoteRepoUrlLabel = new JLabel("Git Remote Url:");
        JLabel remoteBranchLabel = new JLabel("Git Remote Branch:");
        JLabel remoteSshKeyLabel = new JLabel("SSH key:");
        remoteRepoUrl = new MirthTextField();
        remoteBranch = new MirthTextField();
        remoteSshKey = new MirthTextArea();
        uploadSshKeyButton = new MirthButton("Upload ssh key");
        uploadSshKeyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                uploadSshKeyFromFile();
            }
        } );
        remoteRepoUrl.setColumns(50);
        remoteBranch.setColumns(20);

        remoteSshKey.setSize(600, 300);
        remoteSshKey.setWrapStyleWord(true);
        remoteSshKey.setLineWrap(true);

        remoteSshKeyScrollPane = new JScrollPane(remoteSshKey,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        remoteSshKeyScrollPane.setPreferredSize(new Dimension(600,200));
        remoteSshKeyScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        remoteSshKeyScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        validateSettingButton = new MirthButton("Validate Setting");
        validateSettingButton.addActionListener(new ActionListener() {
                @SneakyThrows
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(validateProperties()){
                        plugin.setPropertiesToServer(getProperties());
                        gitServlet = getFrame().mirthClient.getServlet(channelHistoryServletInterface.class);
                        JSONObject valid_res = new JSONObject(gitServlet.validateSetting());
                        if(valid_res.get("validate").equals("fail")){
                            JOptionPane.showMessageDialog(parent, "Error: " + valid_res.get("body"),
                                    "Plugin Info", JOptionPane.ERROR_MESSAGE);
                        }else{
                            JOptionPane.showMessageDialog(parent, valid_res.get("body"),
                                    "Plugin Info", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            }
        );



        vHistoryPanel.add(enableGitCheckbox,"wrap");
        vHistoryPanel.add(remoteRepoUrlLabel);
        vHistoryPanel.add(remoteRepoUrl, "wrap");
        vHistoryPanel.add(remoteBranchLabel);
        vHistoryPanel.add(remoteBranch, "wrap");
        vHistoryPanel.add(remoteSshKeyLabel);
        vHistoryPanel.add(remoteSshKeyScrollPane, "wrap");
        vHistoryPanel.add(uploadSshKeyButton,"wrap");
        vHistoryPanel.add(validateSettingButton);



        add(vHistoryPanel, "growx");
    }
    public void enableFields(boolean isEnabled){
        remoteRepoUrl.setEnabled(isEnabled);
        remoteBranch.setEnabled(isEnabled);
        remoteSshKey.setEnabled(isEnabled);
        uploadSshKeyButton.setEnabled(isEnabled);
        validateSettingButton.setEnabled(isEnabled);
    }
    public void setProperties(Properties properties){
        if (properties.getProperty(VersionControlConstants.VERSION_HISTORY_REMOTE_REPO_URL) != null) {
            remoteRepoUrl.setText(properties.getProperty(VersionControlConstants.VERSION_HISTORY_REMOTE_REPO_URL));
        } else {
            remoteRepoUrl.setText("");
        }
        if (properties.getProperty(VersionControlConstants.VERSION_HISTORY_REMOTE_BRANCH) != null) {
            remoteBranch.setText(properties.getProperty(VersionControlConstants.VERSION_HISTORY_REMOTE_BRANCH));
        } else {
            remoteBranch.setText("");
        }
        if (properties.getProperty(VersionControlConstants.VERSION_HISTORY_REMOTE_SSH_KEY) != null) {
            remoteSshKey.setText(properties.getProperty(VersionControlConstants.VERSION_HISTORY_REMOTE_SSH_KEY));
        } else {
            remoteSshKey.setText("");
        }

        if (Boolean.parseBoolean(properties.getProperty(VersionControlConstants.VERSION_HISTORY_ENABLE))) {
            enableGitCheckbox.setSelected(true);
            enableFields(true);
        } else {
            enableGitCheckbox.setSelected(false);
            enableFields(false);
        }

        repaint();
        this.getFrame().setSaveEnabled(false);
    }
    public Properties getProperties(){
        Properties properties = new Properties();
        properties.setProperty(VersionControlConstants.VERSION_HISTORY_REMOTE_REPO_URL,remoteRepoUrl.getText());
        properties.setProperty(VersionControlConstants.VERSION_HISTORY_REMOTE_BRANCH,remoteBranch.getText());
        properties.setProperty(VersionControlConstants.VERSION_HISTORY_REMOTE_SSH_KEY,remoteSshKey.getText());
        properties.setProperty(VersionControlConstants.VERSION_HISTORY_ENABLE, String.valueOf(enableGitCheckbox.isSelected()));


        return properties;
    }
    public boolean validateProperties(){
        if(remoteRepoUrl.getText().isEmpty()){
            JOptionPane.showMessageDialog(parent, "Git remote url can not be empty!",
                    "Plugin Warning", JOptionPane.ERROR_MESSAGE);
            return false;
        } else if(remoteBranch.getText().isEmpty()){
            JOptionPane.showMessageDialog(parent, "Git remote branch can not be empty!",
                    "Plugin Warning", JOptionPane.ERROR_MESSAGE);
            return false;
        } else if(remoteSshKey.getText().isEmpty()){
            JOptionPane.showMessageDialog(parent, "Git remote ssh key can not be empty!",
                    "Plugin Warning", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    @Override
    public void doRefresh() {
        if (PlatformUI.MIRTH_FRAME.alertRefresh()) {
            return;
        }

        final String workingId = getFrame().startWorking("Loading " + getTabName() + " properties...");

        final Properties serverProperties = new Properties();

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            public Void doInBackground() {
                try {
                    Properties propertiesFromServer = plugin.getPropertiesFromServer();
                    System.out.println(propertiesFromServer.toString());

                    if (propertiesFromServer != null) {
                        serverProperties.putAll(propertiesFromServer);
                    }
                } catch (Exception e) {
                    getFrame().alertThrowable(getFrame(), e);
                }
                return null;
            }

            @Override
            public void done() {
                setProperties(serverProperties);
                getFrame().stopWorking(workingId);
            }
        };

        worker.execute();
    }

    @Override
    public boolean doSave() {

        final String workingId = getFrame().startWorking("Saving " + getTabName() + " properties...");

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            public Void doInBackground() {
                try {
                    if(validateProperties()){
                        plugin.setPropertiesToServer(getProperties());
                        gitServlet = getFrame().mirthClient.getServlet(channelHistoryServletInterface.class);
                        gitServlet.updateSetting();
                    }else {
                        enableGitCheckbox.setSelected(false);
                        enableFields(false);
                    }
                } catch (Exception e) {
                    getFrame().alertThrowable(getFrame(), e);
                }
                return null;
            }

            @Override
            public void done() {
                setSaveEnabled(false);
                getFrame().stopWorking(workingId);
            }
        };

        worker.execute();

        return true;
    }

    public void uploadSshKeyFromFile(){
        String content = this.parent.browseForFileString(null);
        if (content != null) {
            remoteSshKey.setText(content);

        }
    }
}