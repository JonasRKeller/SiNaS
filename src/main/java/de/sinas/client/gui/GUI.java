package de.sinas.client.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringReader;
import java.util.stream.Collectors;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;

import de.sinas.Conversation;
import de.sinas.client.AppClient;
import de.sinas.client.gui.language.Language;
import de.sinas.net.PROTOCOL;

public class GUI extends JFrame {
	private JPanel contentPane;
	private JTextField messageTextField;
	private JList<Conversation> conversationsList;
	private Language lang;
	private AppClient appClient;
	private Conversation currentConversation;
	private JEditorPane messagesPane;

	public GUI(AppClient appClient, Language lang) {
		this.lang = lang;
		this.appClient = appClient;
		setTitle("SiNaS");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 600, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] { 0, 0, 0, 0 };
		gbl_contentPane.rowHeights = new int[] { 0, 0, 0, 0 };
		gbl_contentPane.columnWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
		gbl_contentPane.rowWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
		contentPane.setLayout(gbl_contentPane);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setMinimumSize(new Dimension(100, 100));
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridheight = 2;
		gbc_scrollPane.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 1;
		contentPane.add(scrollPane, gbc_scrollPane);

		conversationsList = new JList<Conversation>();
		conversationsList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				onConversationSelected();
			}
		});
		conversationsList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Conversation conversation = (Conversation) value;
				return super.getListCellRendererComponent(list, "<html>" + conversation.getName() + "</html>", index, isSelected, cellHasFocus);
			}
		});
		conversationsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane.setViewportView(conversationsList);

		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane_1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.gridwidth = 2;
		gbc_scrollPane_1.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane_1.gridx = 1;
		gbc_scrollPane_1.gridy = 1;
		contentPane.add(scrollPane_1, gbc_scrollPane_1);

		messagesPane = new JEditorPane();
		messagesPane.setEditorKit(new HTMLEditorKit());
		messagesPane.setEditable(false);
		scrollPane_1.setViewportView(messagesPane);

		JMenuBar menuBar = new JMenuBar();
		GridBagConstraints gbc_menuBar = new GridBagConstraints();
		gbc_menuBar.anchor = GridBagConstraints.WEST;
		gbc_menuBar.gridwidth = 3;
		gbc_menuBar.insets = new Insets(0, 0, 5, 5);
		gbc_menuBar.gridx = 0;
		gbc_menuBar.gridy = 0;
		contentPane.add(menuBar, gbc_menuBar);
		
		JMenu mnOptions = new JMenu(lang.getString("options"));
		menuBar.add(mnOptions);
		
		JMenuItem mntmAddConversation = new JMenuItem(lang.getString("add_conversation"));
		mntmAddConversation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onAddConversation();
			}
		});
		mnOptions.add(mntmAddConversation);

		JMenu mnHelp = new JMenu(lang.getString("help"));
		menuBar.add(mnHelp);

		JMenuItem mntmAbout = new JMenuItem(lang.getString("about"));
		mnHelp.add(mntmAbout);
		
		JMenu mnConversation = new JMenu(lang.getString("conversation"));
		menuBar.add(mnConversation);
		
		JMenuItem mntmAddUser = new JMenuItem(lang.getString("add_user"));
		mntmAddUser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onConversationAddUser();
			}
		});
		mnConversation.add(mntmAddUser);
		
		JMenuItem mntmRemoveUser = new JMenuItem(lang.getString("remove_user"));
		mntmRemoveUser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onConversationRemoveUser();
			}
		});
		mnConversation.add(mntmRemoveUser);
		
		JMenuItem mntmRename = new JMenuItem(lang.getString("rename"));
		mntmRename.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onConversationRename();
			}
		});
		mnConversation.add(mntmRename);

		messageTextField = new JTextField();
		GridBagConstraints gbc_messageTextField = new GridBagConstraints();
		gbc_messageTextField.insets = new Insets(0, 0, 0, 5);
		gbc_messageTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_messageTextField.gridx = 1;
		gbc_messageTextField.gridy = 2;
		contentPane.add(messageTextField, gbc_messageTextField);
		messageTextField.setColumns(10);

		JButton sendButton = new JButton(lang.getString("send"));
		sendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onSendButton();
			}
		});
		GridBagConstraints gbc_sendButton = new GridBagConstraints();
		gbc_sendButton.gridx = 2;
		gbc_sendButton.gridy = 2;
		contentPane.add(sendButton, gbc_sendButton);

		createUpdateListener();
		createErrorListener();

		appClient.requestConversations();
	}

	private void onAddConversation() {
		String conversationName = JOptionPane.showInputDialog(this, lang.getString("enter_conversation_name"), lang.getString("add_conversation"), JOptionPane.QUESTION_MESSAGE);
		if (conversationName == null || conversationName.equals("")) {
			return;
		}
		appClient.addConversation(conversationName);
	}

	private void onConversationSelected() {
		if (conversationsList.getSelectedValue() == null) {
			return;
		}
		if (conversationsList.getSelectedValue().equals(currentConversation)) {
			return;
		}
		currentConversation = conversationsList.getSelectedValue();
		updateMessagesPane();
		appClient.requestMessages(currentConversation.getId(), 1000);
	}

	private void onSendButton() {
		if (currentConversation == null) {
			return;
		}
		appClient.sendMessage(currentConversation.getId(), messageTextField.getText());
	}

	private void onConversationAddUser() {
		String username = JOptionPane.showInputDialog(this, lang.getString("enter_username"), lang.getString("add_user"), JOptionPane.QUESTION_MESSAGE);
		if (username == null || username.equals("")) {
			return;
		}
		appClient.addUserToConversation(currentConversation.getId(), username);
	}

	private void onConversationRemoveUser() {
		String username = JOptionPane.showInputDialog(this, lang.getString("enter_username"), lang.getString("remove_user"), JOptionPane.QUESTION_MESSAGE);
		if (username == null || username.equals("")) {
			return;
		}
		appClient.removeUserFromConversation(currentConversation.getId(), username);
	}

	private void onConversationRename() {
		String conversationName = JOptionPane.showInputDialog(this, lang.getString("enter_conversation_name"), lang.getString("rename"), JOptionPane.QUESTION_MESSAGE);
		if (conversationName == null || conversationName.equals("")) {
			return;
		}
		appClient.renameConversation(currentConversation.getId(), conversationName);
	}

	private void createUpdateListener() {
		appClient.addUpdateListener(msgBase -> {
			switch (msgBase) {
			case PROTOCOL.SC.CONVERSATION:
				onConversationUpdate();
				break;
			case PROTOCOL.SC.MESSAGES:
				onMessagesUpdate();
				break;
			}
		});
	}

	private void onConversationUpdate() {
		Conversation lastCurrentConversation = currentConversation;
		conversationsList.setListData(appClient.getConversations().toArray(new Conversation[0]));
		for (int i = 0; i < conversationsList.getModel().getSize(); i++) {
			if (conversationsList.getModel().getElementAt(i).equals(lastCurrentConversation)) {
				conversationsList.setSelectedIndex(i);
				break;
			}
		}
	}

	private void onMessagesUpdate() {
		if (currentConversation != null) {
			updateMessagesPane();
		}
	}

	private void createErrorListener() {
		appClient.addErrorListener(errorCode -> {
			JOptionPane.showMessageDialog(this, lang.getString("error_code") + ": " + errorCode, lang.getString("some_error_occurred"), JOptionPane.ERROR_MESSAGE);
		});
	}

	private void updateMessagesPane() {
		String html = "<html><div>" + String.join("<br>", currentConversation.getMessages().stream().map(m -> m.getContent()).collect(Collectors.toList())) + "</div></html>";
		Document doc = messagesPane.getEditorKit().createDefaultDocument();
		try {
			messagesPane.getEditorKit().read(new StringReader(html), doc, 0);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		messagesPane.setDocument(doc);
	}
}
