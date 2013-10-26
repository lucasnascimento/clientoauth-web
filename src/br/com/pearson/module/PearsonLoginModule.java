package br.com.pearson.module;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.Principal;
import java.security.acl.Group;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.activemq.jaas.GroupPrincipal;
import org.apache.activemq.jaas.UserPrincipal;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

@SuppressWarnings("unused")
public class PearsonLoginModule implements LoginModule {

	private Subject subject;
	private CallbackHandler callbackHandler;
	private final Set<Principal> principals = new HashSet<>();
	private Connection conn;
	
	private String db_url,
			db_user,
			db_pass,
			db_driver,
			query_get_user,
			query_add_user,
			query_get_groups;
	private final HashMap<String, String> tokenUrlMap = new HashMap<>(),
			userUrlMap = new HashMap<>(),
			logoutUrlMap = new HashMap<>(),
			providerMap = new HashMap<>(),
			clientIdMap = new HashMap<>(),
			secretMap = new HashMap<>();
			
	
	
	@Override
	public boolean login() throws LoginException {
		if (conn == null) {
			throw new LoginException("Dead database connection..");
		}
		Callback[] callbacks = new Callback[2];
		callbacks[0] = new NameCallback("State: ");
		callbacks[1] = new PasswordCallback("Code: ", true);
		try {
			callbackHandler.handle(callbacks);
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
//			System.out.println(ex.getStackTrace().toString());
			throw new LoginException(ex.getMessage());
		} catch (UnsupportedCallbackException ex) {
			System.err.println(ex.getMessage());
//			System.out.println(ex.getStackTrace().toString());
			return false;
		}
		
		String state = ((NameCallback) callbacks[0]).getName();
		if (state == null) {
			String error = "OAuth provided not set";
//			System.out.println(error);
			throw new FailedLoginException(error);
		} else if (!providerMap.containsKey(state)) {
			String error = "OAuth provided not recognized";
//			System.out.println(error);
			throw new FailedLoginException(error);
		}
		String code;
		try {
			code = new String(((PasswordCallback) callbacks[1]).getPassword());
		} catch (NullPointerException npe) {
			String error = "code is not provided";
//			System.out.println(error);
			throw new FailedLoginException(error);
		}
		
//		sanityCheckUserInput(code, state);
		String accessToken = getAccessToken(state, code);
		String userId = getUserId(state, accessToken);
//		databaseSignIn(state, new String());

		return true;
	}
	
	private void sanityCheckUserInput(String... datas) throws LoginException {
		for (String data : datas) {
			String test_data = data.toLowerCase();
			if (test_data.contains("delete")
					|| test_data.contains("select")
					|| test_data.contains("update")
					|| test_data.contains("alter")
					|| test_data.contains("raise")
					|| test_data.contains("delete")
					|| test_data.contains("drop")) {
				throw new LoginException("User input Failed sanity check :: " + data);
			}
			for (Character c : test_data.toCharArray()) {
				@SuppressWarnings("static-access")
				int type = c.getType(c);
				if (!Character.isDigit(c) && !Character.isAlphabetic(c)) {
					throw new LoginException("Forbidden character in user data :: " + data);
				}
			}
		}
	}
	
	private String getAccessToken(String state, String code) throws FailedLoginException {
		final String accesstokenUrl = tokenUrlMap.get(providerMap.get(state));
		final String clientID = clientIdMap.get(providerMap.get(state));
		final String secret = secretMap.get(providerMap.get(state));
		if (accesstokenUrl == null) {
			String error = "undefined logingprovider provided";
			System.out.println(error);
			throw new FailedLoginException(error);
		}

		String accessToken = postRequest(accesstokenUrl,
				new BasicNameValuePair("client_id", clientID),
//				new BasicNameValuePair("code", code),
				new BasicNameValuePair("client_secret", secret));
		if (accessToken == null) {
			throw new FailedLoginException("Unable to retrieve access token");
		}

		return accessToken;
	}
	
	private String getUserId(String state, String accessToken) throws FailedLoginException {
		String userUrl = userUrlMap.get(providerMap.get(state));

		if (userUrl == null) {
			String error = "undefined logingprovider provided";
//			System.out.println(error);
			throw new FailedLoginException(error);
		}

		String user_id = getRequest(userUrl, "?access_token=" + accessToken);
		if (user_id == null) {
			throw new FailedLoginException("Could not authorice user_id retrieval from access token");
		}
		return user_id;
	}
	
	private void databaseSignIn(String state, String userId) throws FailedLoginException {
		final String fix_query_get_user = query_get_user
				.replace("${provider}", state)
				.replace("${provider_user_id}", userId);
		final String fix_query_add_user = query_add_user
				.replace("${provider}", state)
				.replace("${provider_user_id}", userId);
		final String fix_query_get_groups = query_get_groups
				.replace("${provider}", state)
				.replace("${provider_user_id}", userId);
		boolean autoCommit = false;
		try {
			autoCommit = conn.getAutoCommit();
			if (autoCommit == false) {
				conn.setAutoCommit(true);
			}
			//START TRANSACTION
			conn.setAutoCommit(false);
			Integer pk = null;
			try (PreparedStatement prep = conn.prepareStatement(fix_query_get_user); ResultSet rs = prep.executeQuery()) {
				if (rs.next()) {
					pk = rs.getInt(1);
				}
			}
			//If new user, insert user
			if (pk == null) {
				try (PreparedStatement prep = conn.prepareStatement(fix_query_add_user);) {
					prep.executeUpdate();
					try (ResultSet rs = prep.getGeneratedKeys()) {
						if (rs.next()) {
							pk = rs.getInt(1);
						}
					}
				}
			}
			if (pk == null) {
				throw new FailedLoginException("Failed adding new user to local database");
			}
			//Get user roles
			try (PreparedStatement prep = conn.prepareStatement(fix_query_get_groups); ResultSet rs = prep.executeQuery()) {
				while (rs.next()) {
					principals.add(new GroupPrincipal(rs.getString(1)));
				}
			}

			principals.add(new UserPrincipal(pk.toString()));
			conn.commit();
		} catch (SQLException ex) {
			System.err.println(ex.getMessage());
			System.out.println(ex);
		} finally {
			try {
				conn.setAutoCommit(autoCommit);
			} catch (SQLException ex) {
				System.err.println(ex.getMessage());
				System.out.println(ex);
			}
		}
	}
	
	private String postRequest(String address, BasicNameValuePair... params) {
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(address);
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(params.length);
			for (BasicNameValuePair paramPair : params) {
				nameValuePairs.add(paramPair);
			}
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			HttpResponse response = client.execute(post);
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			String line = "";
			StringBuilder sb = new StringBuilder();
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			String string = sb.toString();

			string = string.split("access_token=")[1];
			string = string.split("&")[0];

			return string;
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
			System.out.println( ex );
		}
		return null;
	}

	/**
	 * get a user ID from a provider using the accessToken
	 *
	 * @param address
	 * @param paramString
	 * @return
	 * @throws Exception
	 */
	private String getRequest(String address, String paramString) {
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(address + paramString);
			HttpResponse response = client.execute(request);

// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			String line = "";
			StringBuilder sb = new StringBuilder();
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			String string = sb.toString();

			string = string.split("\"id\":")[1];
			string = string.split(",")[0];
			return string;
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
			System.out.println( ex);
		}
		return null;
	}
	
	public boolean commit() throws LoginException {
		this.subject.getPrincipals().addAll(this.principals);
		return true;
	}
	

	public boolean abort() throws LoginException {
		this.principals.clear();
		return true;
	}

	public boolean logout() throws LoginException {
//		this.subject.getPrincipals().removeAll(this.principals);
//		this.principals.clear();
		return true;
	}

	
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
		this.subject = (Subject)subject;
		this.callbackHandler = callbackHandler;

		for (String option : options.keySet()) {
			if (option.toLowerCase().endsWith("_provider")) {
				Object tokenUrl = options.get(option.toLowerCase() + "_token");
				Object userUrl = options.get(option.toLowerCase() + "_user");
				Object logoutUrl = options.get(option.toLowerCase() + "_logout");

				Object client_id = options.get(option.toLowerCase() + "_client_id");
				Object secret = options.get(option.toLowerCase() + "_secret");

				if (tokenUrl == null) {
					System.err.println("No 'token url' option set for provider '" + option + "'.");
				
				}else if (userUrl == null) {
					System.err.println("No 'user url' option set for provider '" + option + "'.");
				
				} else if (client_id == null) {
					System.err.println("No 'client_id' option set for provider '" + option + "'.");
				} else if (secret == null) {
					System.err.println("No 'secret' option set for provider '" + option + "'.");
				} else {
					providerMap.put(options.get(option).toString(), option);
					tokenUrlMap.put(option, tokenUrl.toString());
					userUrlMap.put(option, userUrl.toString());
					if (logoutUrl != null) {
						logoutUrlMap.put(option, logoutUrl.toString());
					}
					clientIdMap.put(option, client_id.toString());
					secretMap.put(option, secret.toString());
				}
			}
		}

		this.db_url = options.get("db_url").toString();
		this.db_user = options.get("db_user").toString();
		this.db_pass = options.get("db_pass").toString();
		this.db_driver = options.get("db_driver").toString();

		this.query_add_user = options.get("query_add_user").toString();
		this.query_get_user = options.get("query_get_user").toString();
		this.query_get_groups = options.get("query_get_groups").toString();

		boolean success = false;
		if (db_url == null) {
			System.err.println("Missing option: db_url");
		} else if (db_user == null) {
			System.err.println("Missing option: db_user");
		} else if (db_pass == null) {
			System.err.println("Missing option: db_pass");
		} else if (db_driver == null) {
			System.err.println("Missing option: db_driver");
		} else if (query_get_user == null) {
			System.err.println("Missing option: query_get_user");
		} else if (!query_get_user.toString().contains("${provider}")) {
			System.err.println("Missing option: query_get_user");
		} else if (!query_get_user.toString().contains("${provider_user_id}")) {
			System.err.println("Missing option: query_get_user");
		} else if (query_add_user == null) {
			System.err.println("Missing option: query_add_user");
		} else if (!query_add_user.toString().contains("${provider}")) {
			System.err.println("Missing option: query_add_user");
		} else if (!query_add_user.toString().contains("${provider_user_id}")) {
			System.err.println("Missing option: query_add_user");
		} else if (query_get_groups == null) {
			System.err.println("Missing option: query_get_groups");
		} else if (!query_get_groups.toString().contains("${provider}")) {
			System.err.println("Missing option: query_get_groups");
		} else if (!query_get_groups.toString().contains("${provider_user_id}")) {
			System.err.println("Missing option: query_get_groups");
		} else if (providerMap.isEmpty()) {
			System.err.println("No providers set up with sufficient parameter settings");
		} else {
			try {
				Class.forName(db_driver);
				this.conn = DriverManager.getConnection(db_url, db_user, db_pass);
				success = true;
			} catch (ClassNotFoundException | SQLException ex) {
//				String error = "Could not establish database conenction";
				System.err.println(ex.getMessage());
//				System.out.println(error, ex);
			}
		}
		if (success) {
			System.out.println("Oauth Login Module initialized with configured providers.");
		} else {
			System.err.println("Oauth Login Module failed to initialize.");
		}
		
	}
}
