/*
 * This Source Code Form is subject to the terms of the
 * Mozilla Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.innovarhealthcare.channelHistory.client;

import com.innovarhealthcare.channelHistory.shared.RevisionInfo;
import com.innovarhealthcare.channelHistory.shared.interfaces.channelHistoryServletInterface;
import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.model.codetemplates.CodeTemplate;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Jim(Zi Min) Weng
 * @create 2024-05-07 8:46 AM
 */
public class CodeTemplateHistoryDialog extends JDialog {
    private static Logger log = Logger.getLogger(VersionHistoryTabPanel.class);

    private RevisionInfoTable tblRevisions;

    private channelHistoryServletInterface gitServlet;
    private static DateFormat df = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

    private String cid;

    private JPopupMenu popupMenu;

    private JMenuItem mnuShowDiff;

    private Frame parent = PlatformUI.MIRTH_FRAME;

    private ObjectXMLSerializer serializer;
    public CodeTemplateHistoryDialog(Window parent, String codeTemplateId) {
        super(parent);
        setTitle("Code Template History");
        setPreferredSize(new Dimension(1200, 700));
        Dimension dlgSize = getPreferredSize();
        Dimension frmSize = parent.getSize();
        Point loc = parent.getLocation();

        if ((frmSize.width == 0 && frmSize.height == 0) || (loc.x == 0 && loc.y == 0)) {
            setLocationRelativeTo(null);
        } else {
            setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
        }
        cid = codeTemplateId;

        this.serializer = ObjectXMLSerializer.getInstance();
        setLayout(new BorderLayout());

        tblRevisions = new RevisionInfoTable();
        tblRevisions.setRowSelectionAllowed(true);
        tblRevisions.setColumnSelectionAllowed(false);
        tblRevisions.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        popupMenu = new JPopupMenu();
        mnuShowDiff = new JMenuItem("Show Diff");
        mnuShowDiff.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showDiffWindow();
            }
        });
        popupMenu.add(mnuShowDiff);

        MouseAdapter popupListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handlePopupEvent(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handlePopupEvent(e);
            }

            public void handlePopupEvent(MouseEvent e) {
                if(e.isPopupTrigger()) {
                    //System.out.println("popup triggered");
                    mnuShowDiff.setEnabled(tblRevisions.getSelectedRowCount() == 2);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        };
        tblRevisions.addMouseListener(popupListener);
        JScrollPane scrollPane = new JScrollPane(tblRevisions);
        scrollPane.addMouseListener(popupListener);
        add(scrollPane);
        load();


        pack();
        setModal(true);
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    }
    public void load() {
        this.loadHistory(false);
    }
    public void loadHistory(boolean shouldNotifyOnComplete) {
        SwingUtilities.invokeLater(new CodeTemplateHistoryDialog.LoadGitHistoryRunnable(shouldNotifyOnComplete));
    }
    private void showDiffWindow() {
        popupMenu.setVisible(false);
        int[] rows = tblRevisions.getSelectedRows();
        RevisionInfoTableModel model = (RevisionInfoTableModel)tblRevisions.getModel();
        RevisionInfo ri1 = model.getRevisionAt(rows[0]);
        RevisionInfo ri2 = model.getRevisionAt(rows[1]);

        try {
            String left = gitServlet.getContent(cid, ri1.getHash(), "codetemplate");
            CodeTemplate leftCodeTemplate = parse(left, ri1.getShortHash());
            String right = gitServlet.getContent(cid, ri2.getHash(), "codetemplate");
            CodeTemplate rightCodeTemplate = parse(right, ri2.getShortHash());

            String labelPrefix = leftCodeTemplate.getName();
            String leftLabel = labelPrefix + " Time: " + df.format(new Date(ri1.getTime())) + " Committed by " +ri1.getCommitterName();
            String rightLabel = labelPrefix + " Time: " + df.format(new Date(ri2.getTime())) + " Committed by " +ri1.getCommitterName();

            DiffWindow dw = DiffWindow.create("Code Template Diff", leftLabel, rightLabel, leftCodeTemplate, rightCodeTemplate, left, right, this);
            dw.setSize(parent.getWidth() - 10, parent.getHeight()-10);
            dw.setVisible(true);
        }
        catch(Exception e) {
            PlatformUI.MIRTH_FRAME.alertThrowable(PlatformUI.MIRTH_FRAME, e);
        }
    }
    private CodeTemplate parse(String xml, String rev) {
        CodeTemplate ct = serializer.deserialize(xml, CodeTemplate.class);

        return ct;
    }

    private class LoadGitHistoryRunnable implements Runnable {
        private boolean shouldNotifyOnComplete;

        LoadGitHistoryRunnable(boolean shouldNotifyOnComplete) {
            this.shouldNotifyOnComplete = shouldNotifyOnComplete;
        }

        @Override
        public void run() {
            try {
                // initialize once
                // doing here because do not want to delay the startup of MC client which takes several seconds to start.
                if(gitServlet == null) {
                    gitServlet = parent.mirthClient.getServlet(channelHistoryServletInterface.class);
                }

                // then fetch revisions
                List<String> revisions = gitServlet.getHistory(cid, "Codetemplate");
                RevisionInfoTableModel model = new RevisionInfoTableModel(revisions);
                tblRevisions.setModel(model);

                if (shouldNotifyOnComplete) {
                    PlatformUI.MIRTH_FRAME.alertInformation(parent, "History refreshed!");
                }
            } catch (Exception e) {
                PlatformUI.MIRTH_FRAME.alertThrowable(PlatformUI.MIRTH_FRAME, e);
            }
        }
    }
}