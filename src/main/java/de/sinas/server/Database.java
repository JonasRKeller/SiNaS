package de.sinas.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import de.sinas.Conversation;
import de.sinas.Message;
import de.sinas.User;
import de.sinas.crypto.Encoder;
import de.sinas.crypto.HashHandler;

public class Database {
	private Connection connection;

	public Database(String dbPath, int saltSize) {
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		initEmptyDatabase(saltSize);
	}

	private void initEmptyDatabase(int saltSize) {
		try {
			Statement statement = connection.createStatement();
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS `users` ( `username` TEXT NOT NULL UNIQUE, `password_hash` TEXT NOT NULL, PRIMARY KEY(`username`) )");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS `conversations` ( `conversation_id` TEXT NOT NULL PRIMARY KEY UNIQUE, `name` TEXT NOT NULL )");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS `messages` ( `id` TEXT NOT NULL PRIMARY KEY UNIQUE, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `sender` TEXT NOT NULL, `is_file` REAL NOT NULL, `conversation_id` TEXT NOT NULL, FOREIGN KEY(`sender`) REFERENCES `users`(`username`), FOREIGN KEY(`conversation_id`) REFERENCES `conversations`(`conversation_id`) )");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS `conversations_users` ( `conversation_id` TEXT NOT NULL, `username` TEXT NOT NULL, FOREIGN KEY(`conversation_id`) REFERENCES `conversations`(`conversation_id`), FOREIGN KEY(`username`) REFERENCES `users`(`username`), PRIMARY KEY(`conversation_id`,`username`) )");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS `configuration` ( `salt` TEXT NOT NULL, `id` TINYINT NOT NULL DEFAULT 1 CHECK(id=1), PRIMARY KEY(`id`) )");
			ResultSet rs = statement.executeQuery("SELECT * FROM `configuration` WHERE `id`=1");
			if (!rs.next()) {
				statement.executeUpdate("INSERT INTO `configuration`(`salt`) VALUES ('" + Encoder.b64Encode(new HashHandler().getSecureRandomBytes(saltSize)) + "')");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads the salt bytes.
	 *
	 * @return The salt Bytes. null if an error occurred
	 */
	public byte[] loadSalt() {
		try {
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM `configuration` WHERE `id`=1");
			if (rs.next()) {
				return Encoder.b64Decode(rs.getString("salt"));
			} else {
				throw new IllegalStateException("There is no row in the configuration table!");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Loads the user with the given name with ip and port.<br>
	 * This method is to get a user that connected to the server.<br>
	 * If no user with the given name exists a temporary user is returned.
	 *
	 * @param ip
	 * @param port
	 * @param username
	 * @return The user with the given username
	 */
	public User loadConnectedUser(String username, String ip, int port) {
		try {
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM `users` WHERE `username`=?");
			statement.setString(1, username);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				return new User(ip, port, rs.getString("username"), rs.getString("password_hash"));
			} else {
				return new TempUser(ip, port);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Loads the user with the given name with an empty ip and the port 0.<br>
	 * This method is to get information about a user without having the user to
	 * connect to the server.<br>
	 * If no user with the given name exists a temporary user is returned.
	 *
	 * @param username
	 * @return The user with the given username. {@code null} if no user with the
	 *         given name exists.
	 */
	public User loadUserInfo(String username) {
		return loadConnectedUser(username, "", 0);
	}

	public ArrayList<String> loadAllUsernames() {
		try {
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT `username` FROM `users`;");
			ArrayList<String> result = new ArrayList<>();
			while(rs.next()) {
				result.add(rs.getString("username"));
			}
			return result;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Loads all conversations of the given user<br>
	 * (NOTE: this method only loads the conversations, not the conversation's
	 * messages;<br>
	 * to load messages use {@link #loadMessages(Conversation)})
	 *
	 * @param user
	 * @return all conversations of the given user
	 */
	public ArrayList<Conversation> loadConversations(User user) {
		try {
			ArrayList<Conversation> conversations = new ArrayList<>();
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM conversations_users WHERE username=?");
			statement.setString(1, user.getUsername());
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				conversations.add(loadConversation(rs.getString("conversation_id")));
			}
			return conversations;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Loads the conversation with the given conversation id<br>
	 * (NOTE: this method only loads the conversation, not the conversation's
	 * messages;<br>
	 * to load messages use {@link #loadMessages(Conversation)})
	 *
	 * @param conversationId
	 * @return
	 */
	public Conversation loadConversation(String conversationId) {
		try {
			String id;
			String name;
			ArrayList<String> users = new ArrayList<>();
			{
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM conversations WHERE conversation_id=?");
				statement.setString(1, conversationId);
				ResultSet rs = statement.executeQuery();
				if (rs.next()) {
					id = rs.getString("conversation_id");
					name = rs.getString("name");
				} else {
					return null;
				}
			}
			{
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM conversations_users WHERE conversation_id=?");
				statement.setString(1, conversationId);
				ResultSet rs = statement.executeQuery();
				while (rs.next()) {
					users.add(rs.getString("username"));
				}
			}
			return new Conversation(id, name, users.toArray(new String[0]));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Loads the messages from the given conversation
	 *
	 * @param conversation
	 * @return true if the conversation exists and the messages could be loaded
	 *         successfully, false otherwise
	 */
	public boolean loadMessages(Conversation conversation) {
		try {
			{
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM conversations WHERE conversation_id=?");
				statement.setString(1, conversation.getId());
				ResultSet rs = statement.executeQuery();
				if (!rs.next()) {
					return false;
				}
			}
			{
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM messages WHERE conversation_id=?");
				statement.setString(1, conversation.getId());
				ResultSet rs = statement.executeQuery();
				while (rs.next()) {
					Message message = new Message(rs.getString("id"), rs.getString("content"), rs.getLong("timestamp"),
							rs.getString("sender"), rs.getBoolean("is_file"), rs.getString("conversation_id"));
					conversation.addMessages(message);
				}
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Loads the last {@code amount} messages sent before {@code lastTimestamp} from
	 * the given conversation
	 *
	 * @param conversation
	 * @param lastTimestamp
	 * @param amount
	 * @return true if the conversation exists and the messages could be loaded
	 *         successfully, false otherwise
	 */
	public boolean loadMessages(Conversation conversation, long lastTimestamp, int amount) {
		try {
			{
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM conversations WHERE conversation_id=?");
				statement.setString(1, conversation.getId());
				ResultSet rs = statement.executeQuery();
				if (!rs.next()) {
					return false;
				}
			}
			{
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM messages WHERE conversation_id=? AND timestamp < ? ORDER BY timestamp DESC LIMIT ?");
				statement.setString(1, conversation.getId());
				statement.setLong(2, lastTimestamp);
				statement.setInt(3, amount);
				ResultSet rs = statement.executeQuery();
				while (rs.next()) {
					Message message = new Message(rs.getString("id"), rs.getString("content"), rs.getLong("timestamp"),
							rs.getString("sender"), rs.getBoolean("is_file"), rs.getString("conversation_id"));
					conversation.addMessages(message);
				}
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Creates the given user in the database
	 *
	 * @param user
	 * @return true if the user did not already exist and the user could
	 *         successfully be created, false otherwise
	 */
	public boolean createUser(User user) {
		if (!(loadUserInfo(user.getUsername()) instanceof TempUser)) {
			return false;
		}
		try {
			PreparedStatement statement = connection.prepareStatement("INSERT INTO `users`(`username`,`password_hash`) VALUES (?,?);");
			statement.setString(1, user.getUsername());
			statement.setString(2, user.getPasswordHash());
			statement.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Updates the given user in the database
	 *
	 * @param user
	 * @return true if the user exists and the user could successfully be updated,
	 *         false otherwise
	 */
	public boolean updateUser(User user) {
		if (loadUserInfo(user.getUsername()) != null) {
			return false;
		}
		try {
			PreparedStatement statement = connection.prepareStatement("UPDATE `users` SET `password_hash`=? WHERE `username`=?;");
			statement.setString(1, user.getPasswordHash());
			statement.setString(2, user.getUsername());
			statement.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Creates the given conversation in the database<br>
	 * (NOTE: this method only creates the conversation, not the conversation's
	 * messages;<br>
	 * to create messages use {@link #createMessage(Message)})
	 *
	 * @param conversation
	 * @return true if the conversation did not already exist, false otherwise
	 */
	public boolean createConversation(Conversation conversation) {
		try {
			{
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM conversations WHERE conversation_id=?");
				statement.setString(1, conversation.getId());
				ResultSet rs = statement.executeQuery();
				if (rs.next()) {
					return false;
				}
			}
			PreparedStatement statement = connection.prepareStatement("INSERT INTO `conversations`(`conversation_id`,`name`) VALUES (?,?);");
			statement.setString(1, conversation.getId());
			statement.setString(2, conversation.getName());
			statement.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Updates the given conversation in the database<br>
	 * (NOTE: this method only updates the conversation, not the conversation's
	 * messages)
	 *
	 * @param conversation
	 * @return true if the conversation exists, false otherwise
	 */
	public boolean updateConversation(Conversation conversation) {
		try {
			{
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM conversations WHERE conversation_id=?");
				statement.setString(1, conversation.getId());
				ResultSet rs = statement.executeQuery();
				if (!rs.next()) {
					return false;
				}
			}
			PreparedStatement statement = connection.prepareStatement("UPDATE `conversations` SET `name`=? WHERE conversation_id=?;");
			statement.setString(1, conversation.getName());
			statement.setString(2, conversation.getId());
			statement.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Creates the given message in the database
	 *
	 * @param message
	 * @return true if the message did not already exist, false otherwise
	 */
	public boolean createMessage(Message message) {
		try {
			{
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM messages WHERE id=?");
				statement.setString(1, message.getId());
				ResultSet rs = statement.executeQuery();
				if (rs.next()) {
					return false;
				}
			}
			PreparedStatement statement = connection.prepareStatement("INSERT INTO `messages`(`id`, `content`, `timestamp`, `sender`, `is_file`, `conversation_id`) VALUES (?,?,?,?,?,?);");
			statement.setString(1, message.getId());
			statement.setString(2, message.getContent());
			statement.setLong(3, message.getTimestamp());
			statement.setString(4, message.getSender());
			statement.setBoolean(5, message.isFile());
			statement.setString(6, message.getConversationId());
			statement.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Adds the given user to the given conversation
	 *
	 * @param conversation
	 * @param user
	 * @return true if both the conversation and the user exist and the user is not
	 *         already in the conversation, false otherwise
	 */
	public boolean addUserToConversation(Conversation conversation, String user) {
		try {
			{
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM conversations WHERE conversation_id=?");
				statement.setString(1, conversation.getId());
				ResultSet rs = statement.executeQuery();
				if (!rs.next()) {
					return false;
				}
			}
			{
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE username=?");
				statement.setString(1, user);
				ResultSet rs = statement.executeQuery();
				if (!rs.next()) {
					return false;
				}
			}
			{
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM conversations_users WHERE conversation_id=? AND username=?");
				statement.setString(1, conversation.getId());
				statement.setString(2, user);
				ResultSet rs = statement.executeQuery();
				if (rs.next()) {
					return false;
				}
			}
			PreparedStatement statement = connection.prepareStatement("INSERT INTO `conversations_users`(`conversation_id`,`username`) VALUES (?,?);");
			statement.setString(1, conversation.getId());
			statement.setString(2, user);
			statement.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Removes the given user from the given conversation
	 *
	 * @param conversation
	 * @param user
	 * @return true if both the conversation and the user exist and the user was in
	 *         the conversation, false otherwise
	 */
	public boolean removeUserFromConversation(Conversation conversation, String user) {
		try {
			{
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM conversations WHERE conversation_id=?");
				statement.setString(1, conversation.getId());
				ResultSet rs = statement.executeQuery();
				if (!rs.next()) {
					return false;
				}
			}
			{
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE username=?");
				statement.setString(1, user);
				ResultSet rs = statement.executeQuery();
				if (!rs.next()) {
					return false;
				}
			}
			{
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM conversations_users WHERE conversation_id=? AND username=?");
				statement.setString(1, conversation.getId());
				statement.setString(2, user);
				ResultSet rs = statement.executeQuery();
				if (!rs.next()) {
					return false;
				}
			}
			PreparedStatement statement = connection.prepareStatement("DELETE FROM `conversations_users` WHERE `conversation_id`=? AND `username`=?;");
			statement.setString(1, conversation.getId());
			statement.setString(2, user);
			statement.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
}
