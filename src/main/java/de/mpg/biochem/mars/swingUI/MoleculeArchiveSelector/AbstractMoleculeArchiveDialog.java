/*-
 * #%L
 * Molecule Archive Suite (Mars) - core data storage and processing algorithms.
 * %%
 * Copyright (C) 2018 - 2025 Karl Duderstadt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.mpg.biochem.mars.swingUI.MoleculeArchiveSelector;

import de.mpg.biochem.mars.io.MoleculeArchiveIOFactory;
import de.mpg.biochem.mars.io.MoleculeArchiveSource;
import de.mpg.biochem.mars.io.MoleculeArchiveStorage;
import ij.IJ;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import org.scijava.prefs.PrefService;
import org.scijava.ui.UIService;
import se.sawano.java.text.AlphanumericComparator;

import com.formdev.flatlaf.util.UIScale;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.*;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

abstract public class AbstractMoleculeArchiveDialog implements TreeWillExpandListener {

    protected Consumer<MoleculeArchiveSelection> okCallback;

    protected JFrame dialog;

    protected JTextField containerPathText;

    protected JTree containerTree;

    protected JList recentList;

    protected List<String> recentURLs;

    protected JButton browseBtn;

    protected JButton detectBtn;

    protected JButton clearRecentBtn;

    protected JLabel messageLabel;

    protected JButton okBtn;

    protected JButton cancelBtn;

    protected DefaultTreeModel treeModel;

    protected String lastBrowsePath;

    protected ExecutorService loaderExecutor;

    protected final String initialContainerPath;

    protected Consumer<String> containerPathUpdateCallback;

    protected Consumer<Void> cancelCallback;

    protected TreeCellRenderer treeRenderer;

    protected MoleculeArchiveSwingTreeNode rootNode;

    protected ExecutorService parseExec;

    protected MoleculeArchiveSource source;

    protected final AlphanumericComparator comp = new AlphanumericComparator(Collator.getInstance());

    @Parameter
    protected PrefService prefService;

    @Parameter
    protected UIService uiService;

    public AbstractMoleculeArchiveDialog(Context context) {
        context.inject(this);
        this.initialContainerPath = "";
    }

    public AbstractMoleculeArchiveDialog(String url, Context context) {
        context.inject(this);
        this.initialContainerPath = url;
    }

    public void setTreeRenderer(final TreeCellRenderer treeRenderer) {

        this.treeRenderer = treeRenderer;
    }

    public void setCancelCallback(final Consumer<Void> cancelCallback) {

        this.cancelCallback = cancelCallback;
    }

    public void setContainerPathUpdateCallback(final Consumer<String> containerPathUpdateCallback) {

        this.containerPathUpdateCallback = containerPathUpdateCallback;
    }
    public void setMessage(final String message) {

        messageLabel.setText(message);
    }

    public String getPath() {

        return containerPathText.getText().trim();
    }

    public void run(final Consumer<MoleculeArchiveSelection> okCallback) {

        this.okCallback = okCallback;
        dialog = buildDialog();

        browseBtn.addActionListener(e -> openContainer(this::openBrowseDialog));
        detectBtn.addActionListener(e -> openContainer(() -> getPath()));
        clearRecentBtn.addActionListener(e -> clearRecent());

        // ok and cancel buttons
        okBtn.addActionListener(e -> ok());
        cancelBtn.addActionListener(e -> cancel());
        dialog.setVisible(true);
    }

    private static final int DEFAULT_OUTER_PAD = 8;
    private static final int DEFAULT_BUTTON_PAD = 3;
    private static final int DEFAULT_MID_PAD = 5;

    protected JFrame buildDialog() {

        final int OUTER_PAD = DEFAULT_OUTER_PAD;
        final int BUTTON_PAD = DEFAULT_BUTTON_PAD;
        final int MID_PAD = DEFAULT_MID_PAD;

        final int frameSizeX = UIScale.scale( 800 );
        final int frameSizeY = UIScale.scale( 600 );

        dialog = new JFrame();
        dialog.setPreferredSize(new Dimension(frameSizeX, frameSizeY));
        dialog.setMinimumSize(dialog.getPreferredSize());

        final Container pane = dialog.getContentPane();
        //final JTabbedPane tabs = new JTabbedPane();

        final JPanel browsePanel = new JPanel(false);
        browsePanel.setLayout(new GridBagLayout());
        pane.add( browsePanel );

        recentList = new JList();
        recentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScroller = new JScrollPane(recentList);

        containerPathText = new JTextField();
        containerPathText.setText(initialContainerPath);
        containerPathText.setPreferredSize(new Dimension(frameSizeX / 3, containerPathText.getPreferredSize().height));
        containerPathText.addActionListener(e -> openContainer(() -> getPath()));

        recentList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                JList list = (JList)evt.getSource();
                if (evt.getClickCount() == 2) {
                    // Double-click detected
                    containerPathText.setText((String) list.getSelectedValue());
                }
            }
        });

        final GridBagConstraints ctxt = new GridBagConstraints();
        ctxt.gridx = 0;
        ctxt.gridy = 0;
        ctxt.gridwidth = 3;
        ctxt.gridheight = 1;
        ctxt.weightx = 1.0;
        ctxt.weighty = 0.0;
        ctxt.fill = GridBagConstraints.HORIZONTAL;
        ctxt.insets = new Insets(OUTER_PAD, OUTER_PAD, MID_PAD, BUTTON_PAD);
        browsePanel.add(containerPathText, ctxt);

        browseBtn = new JButton("Browse local");
        final GridBagConstraints cbrowse = new GridBagConstraints();
        cbrowse.gridx = 3;
        cbrowse.gridy = 0;
        cbrowse.gridwidth = 1;
        cbrowse.gridheight = 1;
        cbrowse.weightx = 0.0;
        cbrowse.weighty = 0.0;
        cbrowse.fill = GridBagConstraints.HORIZONTAL;
        cbrowse.insets = new Insets(OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD);
        browsePanel.add(browseBtn, cbrowse);

        detectBtn = new JButton("Load path");
        final GridBagConstraints cdetect = new GridBagConstraints();
        cdetect.gridx = 4;
        cdetect.gridy = 0;
        cdetect.gridwidth = 1;
        cdetect.gridheight = 1;
        cdetect.weightx = 0.0;
        cdetect.weighty = 0.0;
        cdetect.fill = GridBagConstraints.HORIZONTAL;
        cdetect.insets = new Insets(OUTER_PAD, BUTTON_PAD, MID_PAD, OUTER_PAD);
        browsePanel.add(detectBtn, cdetect);

        clearRecentBtn = new JButton("Clear recent");
        final GridBagConstraints cClear = new GridBagConstraints();
        cClear.gridx = 5;
        cClear.gridy = 0;
        cClear.gridwidth = 1;
        cClear.gridheight = 1;
        cClear.weightx = 0.0;
        cClear.weighty = 0.0;
        cClear.fill = GridBagConstraints.HORIZONTAL;
        cClear.insets = new Insets(OUTER_PAD, BUTTON_PAD, MID_PAD, OUTER_PAD);
        browsePanel.add(clearRecentBtn, cClear);

        final GridBagConstraints ctree = new GridBagConstraints();
        ctree.gridx = 0;
        ctree.gridy = 1;
        ctree.gridwidth = 6;
        ctree.gridheight = 3;
        ctree.weightx = 1.0;
        ctree.weighty = 1.0;
        ctree.ipadx = 0;
        ctree.ipady = 0;
        ctree.insets = new Insets(0, OUTER_PAD, 0, OUTER_PAD);
        ctree.fill = GridBagConstraints.BOTH;

        treeModel = new DefaultTreeModel(null);
        containerTree = new JTree(treeModel);
        containerTree.setMinimumSize(new Dimension(350, 230));
        containerTree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);

        containerTree.addTreeWillExpandListener(this);

        if (treeRenderer != null)
            containerTree.setCellRenderer(treeRenderer);

        final JScrollPane treeScroller = new JScrollPane(containerTree);
        treeScroller.setViewportView(containerTree);
        treeScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroller, listScroller);
        browsePanel.add(split, ctree);

        // bottom button
        final GridBagConstraints cbot = new GridBagConstraints();
        cbot.gridx = 0;
        cbot.gridy = 4;
        cbot.gridwidth = 1;
        cbot.gridheight = 1;
        cbot.weightx = 0.0;
        cbot.weighty = 0.0;
        cbot.insets = new Insets(OUTER_PAD, OUTER_PAD, OUTER_PAD, OUTER_PAD);
        cbot.anchor = GridBagConstraints.CENTER;

        messageLabel = new JLabel("");
        messageLabel.setVisible(false);
        cbot.gridx = 0;
        cbot.anchor = GridBagConstraints.CENTER;
        browsePanel.add(messageLabel, cbot);

        okBtn = new JButton("Ok");
        cbot.gridx = 4;
        cbot.ipadx = (int)(20);
        cbot.anchor = GridBagConstraints.EAST;
        cbot.fill = GridBagConstraints.HORIZONTAL;
        cbot.insets = new Insets(MID_PAD, OUTER_PAD, OUTER_PAD, BUTTON_PAD);
        browsePanel.add(okBtn, cbot);

        cancelBtn = new JButton("Cancel");
        cbot.gridx = 5;
        cbot.ipadx = 0;
        cbot.anchor = GridBagConstraints.EAST;
        cbot.fill = GridBagConstraints.HORIZONTAL;
        cbot.insets = new Insets(MID_PAD, BUTTON_PAD, OUTER_PAD, OUTER_PAD);
        browsePanel.add(cancelBtn, cbot);

        //containerTree.addMouseListener( new MarsNodePopupMenu(this).getPopupListener() );

        dialog.pack();
        SwingUtilities.invokeLater(() -> split.setDividerLocation(split.getSize().width
                    - split.getInsets().right
                    - split.getDividerSize()
                    - 300));
        return dialog;
    }

    public JTree getJTree() {
        return containerTree;
    }

    private String openBrowseDialog() {

        final JFileChooser fileChooser = new JFileChooser();
        /*
         *  Need to allow files so h5 containers can be opened,
         *  and directories so that filesystem n5's and zarrs can be opened.
         */
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        if (lastBrowsePath != null && !lastBrowsePath.isEmpty())
            fileChooser.setCurrentDirectory(new File(lastBrowsePath));
        else if (initialContainerPath != null && !initialContainerPath.isEmpty())
            fileChooser.setCurrentDirectory(new File(initialContainerPath));
        else if (IJ.getInstance() != null) {
            File f = null;

            final String currDir = IJ.getDirectory("current");
            final String homeDir = IJ.getDirectory("home");
            if( currDir != null )
                f = new File( currDir );
            else if( homeDir != null )
                f = new File( homeDir );

            fileChooser.setCurrentDirectory(f);
        }

        final int ret = fileChooser.showOpenDialog(dialog);
        if (ret != JFileChooser.APPROVE_OPTION)
            return null;

        final String path = fileChooser.getSelectedFile().getAbsolutePath();
        containerPathText.setText(path);
        lastBrowsePath = path;

        // callback after browse as well
        containerPathUpdateCallback.accept(path);

        return path;
    }

    private void openContainer(final Supplier<String> opener) {

        SwingUtilities.invokeLater(() -> {
            messageLabel.setText("Building tree...");
            messageLabel.setVisible(true);
            messageLabel.repaint();
        });

        final String path = opener.get();
        containerPathUpdateCallback.accept(path);

        if (path == null) {
            messageLabel.setVisible(false);
            dialog.repaint();
            return;
        }

        source = null;
        try {
            source = new MoleculeArchiveIOFactory().openSource(path);
        } catch (IOException e) { e.printStackTrace(); }

        if (source == null) {
            messageLabel.setVisible(false);
            dialog.repaint();
            return;
        }

        final String rootPath = source.getPath();

        if (loaderExecutor == null) {
            loaderExecutor = Executors.newCachedThreadPool();
        }

        final String[] pathParts = path.split( source.getGroupSeparator() );

        final String rootName = pathParts[ pathParts.length - 1 ];
        if( treeRenderer != null  && treeRenderer instanceof MoleculeArchiveTreeCellRenderer )
            ((MoleculeArchiveTreeCellRenderer)treeRenderer ).setRootName(rootName);

        rootNode = new MoleculeArchiveSwingTreeNode( rootPath, treeModel );
        rootNode.setLeaf(false);
        treeModel.setRoot(rootNode);

        containerTree.setEnabled(true);
        containerTree.repaint();

        final MoleculeArchiveSource finalSource = source;

        parseExec = Executors.newSingleThreadExecutor();
        parseExec.submit(() -> {
            try {
                SwingUtilities.invokeLater(() -> {
                    messageLabel.setText("Loading...");
                    messageLabel.repaint();
                });

                addNodePaths(rootNode);
                containerTree.expandRow( 0 );

                SwingUtilities.invokeLater(() -> {
                    messageLabel.setText("Done");
                    messageLabel.repaint();
                });

                Thread.sleep(1000);
                SwingUtilities.invokeLater(() -> {
                    messageLabel.setText("");
                    messageLabel.setVisible(false);
                    messageLabel.repaint();
                });
            }
            catch (InterruptedException e) { }
        });
    }

    private void addNodePaths(MoleculeArchiveSwingTreeNode node) {
        if (node.isLeaf()) return;
        if (node.getChildCount() > 0) return;

        try {
            String treePath = node.getPath();
            String fullPath = (treePath.startsWith("/")) ? treePath : source.getGroupSeparator() + treePath;
            String[] directoryPaths = source.listDirectories(fullPath);
            for (String dir : directoryPaths) {
                String[] parts = dir.split("/");
                MoleculeArchiveSwingTreeNode child = node.addChildPath(parts[parts.length - 1]);
                if (dir.endsWith("." + MoleculeArchiveStorage.MOLECULE_ARCHIVE_STORE_ENDING)
                        || dir.endsWith("." + MoleculeArchiveStorage.N5_DATASET_DIRECTORY_ENDING))
                    child.setLeaf(true);
                else
                    child.setLeaf(false);
            }
            String[] filePaths = source.listFiles(fullPath);
            for (String file : filePaths) {
                String[] parts = file.split("/");
                MoleculeArchiveSwingTreeNode child = node.addChildPath(parts[parts.length - 1]);
                child.setLeaf(true);
            }
            sortRecursive(node);
            //containerTree.repaint();
        } catch (IOException error) {
            throw new RuntimeException(error);
        }
    }

    public void close() {
        // stop parsing things
        if( parseExec != null )
            parseExec.shutdownNow();

        if (source != null) source.close();
        dialog.setVisible(false);
        dialog.dispose();
    }

    public void cancel() {
        close();

        if (cancelCallback != null)
            cancelCallback.accept(null);
    }

    private void sortRecursive( final MoleculeArchiveSwingTreeNode node )
    {
        if( node != null ) {
            final List<MoleculeArchiveTreeNode> children = node.childrenList();
            if( !children.isEmpty())
            {
                children.sort(Comparator.comparing(MoleculeArchiveTreeNode::toString, comp));
            }
            treeModel.nodeStructureChanged(node);
            for( MoleculeArchiveTreeNode child : children )
                sortRecursive( (MoleculeArchiveSwingTreeNode)child );
        }
    }

    private static String normalDatasetName(final String fullPath, final String groupSeparator) {

        return fullPath.replaceAll("(^" + groupSeparator + "*)|(" + groupSeparator + "*$)", "");
    }

    private static boolean pathsEqual( final String a, final String b )
    {
        return normalDatasetName( a, "/" ).equals( normalDatasetName( b, "/" ) );
    }

    @Override
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        parseExec = Executors.newSingleThreadExecutor();
        parseExec.submit(() -> {
            try {
                SwingUtilities.invokeLater(() -> {
                    messageLabel.setText("Loading...");
                    messageLabel.repaint();
                });

                addNodePaths((MoleculeArchiveSwingTreeNode) event.getPath().getLastPathComponent());
                SwingUtilities.invokeLater(() -> {
                    messageLabel.setText("Done");
                    messageLabel.repaint();
                });

                Thread.sleep(1000);
                SwingUtilities.invokeLater(() -> {
                    messageLabel.setText("");
                    messageLabel.setVisible(false);
                    messageLabel.repaint();
                });
            }
            catch (InterruptedException e) { }
        });
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
        //No implementation required.
    }

    abstract public void clearRecent();

    abstract public void ok();
}
