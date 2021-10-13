import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.stream.Collector.Characteristics;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.xml.stream.events.Characters;

import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.io.*;
public class Client 
{
	private static volatile DataOutputStream postToServer = null;
	private static volatile BufferedReader getFromServer = null;
	private static volatile Socket clientFriendSocket = null;
	public static void main(String args[]) throws Exception 
	{
		Screen clientGUI = new Screen();
	}

	public static class Screen extends JFrame implements KeyListener
	{
		private JMenu menuFile, menuHelp;
		private JMenuItem menuItemKey, menuItemConnect, menuItemDisconnect, menuItemExit, menuItemDeveloper;
		private JMenuBar menuBar;
		public JTextField messageTextBox;
		public JTextArea chatScreen, onlineUserScreen;
		public String username = null;
		public ArrayList<String> onlineFriendsList = new ArrayList<>();
		public volatile HashMap<String,PublicKey> publicKeyDictionary = new HashMap<>();
		private volatile RSACryptography hukoCrypto;
		public volatile boolean isKeyGenerated = false;
		public volatile boolean isNetworkConnectionEnabled = false;
		Thread myThread;
		public Screen()
		{
			MenuBarSettings();
			DisplayScreenSettings();
			ApplicationNameSettings();
			MessageInputSettings();
			ChatScreenSettings();
			GroupMemberSettings();
			ActionListenerSettings();
			HurkanUgurThreadExecution();
		}
		private void HurkanUgurThreadExecution()
		{
			new Thread(new Runnable() 
			{
				@Override
				public void run() 
				{
					while (true) 
					{
						//GET YOUR FRIENDS' MESSAGES BY THIS THREAD
						if(isNetworkConnectionEnabled == true)
						{
							try 
							{
								String messageFromOthers = getFromServer.readLine();
								
								if(messageFromOthers.startsWith("Online Group Members: "))
								{
									//SEND YOUR PUBLIC KEY TO EVERYONE AGAIN !!!
									postToServer.writeBytes("$$$" + hukoCrypto.ConvertPublicKeyToString(hukoCrypto.getPublicKey()) + '\n');
									FriendsJoinedGroup(messageFromOthers);
								}
								else if(messageFromOthers.endsWith("has connected!"))
								{
									IncomingMessageFunction(messageFromOthers);
								}
								else if(messageFromOthers.endsWith("has disconnected!"))
								{
									IncomingMessageFunction(messageFromOthers);
									FriendsLeftGroup(messageFromOthers);
								}
								else if(messageFromOthers.contains("$$$"))
								{
									String userName = WhereDidMessageCameFrom(messageFromOthers);
									if(!publicKeyDictionary.containsKey(username))
									{
										String userPublicKey = (messageFromOthers.substring(messageFromOthers.indexOf("$$$")+3)).replace("\n", "");
										publicKeyDictionary.put(userName, hukoCrypto.getPublicKeyFromString(userPublicKey));
									}	
								}
								//RECEIVING PRIVATE MESSAGE FROM SOMEONE
								else if(messageFromOthers.contains("[PRIVATE] "))
								{
									String senderName = messageFromOthers.substring(0, messageFromOthers.indexOf(" ")+1);
									String temp = messageFromOthers.substring(messageFromOthers.indexOf("[PRIVATE] ")+10);
									String privateReceiverName = temp.substring(0, temp.indexOf(" "));
									if(privateReceiverName.equals(username))
									{
										String ciphertext = (temp.substring(temp.indexOf(" ")+1)).replace("\n", "");
										for(PublicKey senderPublicKey : publicKeyDictionary.values())
										{
											String plaintext = hukoCrypto.decryptText(ciphertext, senderPublicKey);
											if(plaintext != null)
											{
												messageFromOthers = senderName + "[PRIVATE] " + plaintext;
												IncomingMessageFunction(messageFromOthers);
												break;
											}
										}	
									}
								}
								//IF IT IS A REGULAR MESSAGE, THEN USE THIS
								else
								{							
									String senderName = messageFromOthers.substring(0, messageFromOthers.indexOf(" ")+1);
									String ciphertext = (messageFromOthers.substring(messageFromOthers.indexOf(" ")+1)).replace("\n", "");
									for(PublicKey senderPublicKey : publicKeyDictionary.values())
									{
										String plaintext =  hukoCrypto.decryptText(ciphertext, senderPublicKey);
										if(plaintext != null)
										{
											messageFromOthers = senderName + plaintext;
											IncomingMessageFunction(messageFromOthers);
											break;
										}
									}	
								}
							} 
							catch (Exception e) {}
						}
					}
				}
			}).start();
		}	
		private void DisplayScreenSettings()
		{
			//JFRAME SETTINGS
			this.setTitle("Hukonet");
			this.setSize(700, 600);
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			this.getContentPane().setBackground(Color.BLACK);
			this.setLocationRelativeTo(null);
			this.setLayout(null);
			this.setVisible(true);
			this.setResizable(false);
		}
		private void ActionListenerSettings()
		{
			//LISTENER SETTINGS
			HurkanListener Handle = new HurkanListener();
			menuItemKey.addActionListener(Handle);
			menuItemConnect.addActionListener(Handle);
			menuItemDisconnect.addActionListener(Handle);
			menuItemExit.addActionListener(Handle);
			menuItemDeveloper.addActionListener(Handle);
			messageTextBox.addKeyListener(this);
		}
		private void MenuBarSettings()
		{
			//JMENU SETTINGS
			menuFile = new JMenu("File");
			menuHelp = new JMenu("Help");
			menuItemKey = new JMenuItem("Generate Keys");
			menuItemConnect = new JMenuItem("Connect to Network");
			menuItemDisconnect = new JMenuItem("Disconnect from Network");
			menuItemDeveloper = new JMenuItem("Developer Information");
			menuItemExit = new JMenuItem("Exit");
			
			menuItemKey.setBackground(Color.RED);
			menuItemKey.setForeground(Color.BLACK);
			menuItemKey.setFont(new Font("Cooper T.",16,16));
			
			menuItemConnect.setBackground(Color.RED);
			menuItemConnect.setForeground(Color.BLACK);
			menuItemConnect.setFont(new Font("Cooper T.",16,16));

			menuItemDisconnect.setBackground(Color.RED);
			menuItemDisconnect.setForeground(Color.BLACK);
			menuItemDisconnect.setFont(new Font("Cooper T.",16,16));
			
			menuItemDeveloper.setBackground(Color.RED);
			menuItemDeveloper.setForeground(Color.BLACK);
			menuItemDeveloper.setFont(new Font("Cooper T.",16,16));
			
			menuItemExit.setBackground(Color.RED);
			menuItemExit.setForeground(Color.BLACK);
			menuItemExit.setFont(new Font("Cooper T.",16,16));
			
			menuFile.add(menuItemKey);
			menuFile.add(menuItemConnect);
			menuFile.add(menuItemDisconnect);
			menuFile.add(menuItemExit);
			menuHelp.add(menuItemDeveloper);
			
			menuBar = new JMenuBar();
			menuBar.add(menuFile);
			menuBar.add(menuHelp);
			menuBar.setBackground(Color.RED);
			menuBar.setForeground(Color.BLACK);	
			this.setJMenuBar(menuBar);
		}
		private void ApplicationNameSettings()
		{
			JLabel nameLabel = new JLabel("ENCRYPTED CHAT APPLICATION");
			nameLabel.setForeground(Color.RED);
			nameLabel.setBackground(Color.BLACK);
			nameLabel.setSize(this.getWidth(),25);
			nameLabel.setLocation(0, 20);
			nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
			nameLabel.setFont(new Font("Cooper T.",25,25));
			this.add(nameLabel);
		}
		private void MessageInputSettings()
		{
			//LABEL
			JLabel inputLabel = new JLabel("Enter a message");
			inputLabel.setForeground(Color.RED);
			inputLabel.setBackground(Color.BLACK);
			inputLabel.setSize(this.getWidth()/3,25);
			inputLabel.setLocation(0, 80);
			inputLabel.setHorizontalAlignment(SwingConstants.CENTER);
			inputLabel.setFont(new Font("Cooper T.",25,25));
			this.add(inputLabel);
			
			//TEXTBOX
			messageTextBox = new JTextField();
			messageTextBox.setSize(430,40);
			messageTextBox.setLocation(this.getWidth()/3, 75);
			messageTextBox.setBackground(Color.RED);
			messageTextBox.setForeground(Color.BLACK);
			messageTextBox.setHorizontalAlignment(SwingConstants.CENTER);
			messageTextBox.setFont(new Font("Cooper T.",12,20));
			this.add(messageTextBox);
			
			//FOCUS ON TEXTBOX
			SwingUtilities.invokeLater( new Runnable() { 
				public void run() { 
					messageTextBox.requestFocus(); 
				    } 
				} );
		}
		private void ChatScreenSettings()
		{
			chatScreen = new JTextArea();
			chatScreen.setBackground(Color.RED);
			chatScreen.setForeground(Color.BLACK);
			chatScreen.setFont(new Font("Cooper T.",18,18));
			chatScreen.setEditable(false);
			chatScreen.setLineWrap(true);
			chatScreen.setWrapStyleWord(true);
			
			JScrollPane scrollBar = new JScrollPane(chatScreen);
			this.add(scrollBar);
			scrollBar.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollBar.setSize(400,350);
			scrollBar.setLocation(17, 150);
	        this.getContentPane().add(scrollBar);
		}
		private void GroupMemberSettings()
		{
			//LABEL
			JLabel onlineUsersLabel = new JLabel("Online Users");
			onlineUsersLabel.setForeground(Color.RED);
			onlineUsersLabel.setBackground(Color.BLACK);
			onlineUsersLabel.setSize(200,50);
			onlineUsersLabel.setLocation(450, 145);
			onlineUsersLabel.setHorizontalAlignment(SwingConstants.CENTER);
			onlineUsersLabel.setFont(new Font("Cooper T.",25,25));
			this.add(onlineUsersLabel);
			
			onlineUserScreen = new JTextArea();
			onlineUserScreen.setBackground(Color.RED);
			onlineUserScreen.setForeground(Color.BLACK);
			onlineUserScreen.setFont(new Font("Cooper T.",18,18));
			onlineUserScreen.setEditable(false);
			onlineUserScreen.setLineWrap(true);
			onlineUserScreen.setWrapStyleWord(true);
			
			JScrollPane scrollBar = new JScrollPane(onlineUserScreen);
			this.add(scrollBar);
			scrollBar.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollBar.setSize(200,298);
			scrollBar.setLocation(450, 200);
	        this.getContentPane().add(scrollBar);
		}
		public void IncomingMessageFunction(String newMessage)
		{
			chatScreen.setText(chatScreen.getText() + "\n" + newMessage);
		}
		public void FriendsJoinedGroup(String newFriend)
		{
			onlineFriendsList.clear();
			newFriend = newFriend.substring(23);
			newFriend = newFriend.replace(username, username + " (Me)");
			int i = 0;
			while(true)
			{
				if(newFriend.contains(", "))
				{
					onlineFriendsList.add(newFriend.substring(0, newFriend.indexOf(", ")));
					newFriend = newFriend.substring(newFriend.indexOf(", ")+2);
					if(i == 0)
						onlineUserScreen.setText(onlineFriendsList.get(i));
					else
						onlineUserScreen.setText((onlineUserScreen.getText() + "\n" + onlineFriendsList.get(i)));
					i++;
				}
				else
				{
					if(i == 0)
					{
						onlineFriendsList.add(newFriend.substring(0, newFriend.indexOf("]")));
						onlineUserScreen.setText(onlineFriendsList.get(i));
						break;
					}
					else
					{
						onlineFriendsList.add(newFriend.substring(0, newFriend.indexOf("]")));
						onlineUserScreen.setText((onlineUserScreen.getText() + "\n" + onlineFriendsList.get(i)));
						break;
					}
				}
			}
		}
		public void FriendsLeftGroup(String oldFriend)
		{
			oldFriend = oldFriend.substring(0, oldFriend.indexOf(" has disconnected!"));
			onlineFriendsList.remove(oldFriend);
			onlineUserScreen.setText("");
			int i=0;
			for(String onlineUser : onlineFriendsList)
			{
				if(i == 0)
					onlineUserScreen.setText(onlineUser);
				else
					onlineUserScreen.setText(onlineUserScreen.getText() + "\n" + onlineUser);
				i++;
			}
				
		}
		public String WhereDidMessageCameFrom(String incomingMessage)
		{
			return incomingMessage.substring(0, incomingMessage.indexOf(":"));
		}
		private class HurkanListener implements ActionListener
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				if(e.getSource() == Screen.this.menuItemKey)
				{
					if(isKeyGenerated == false)
					{
						hukoCrypto = new RSACryptography();
						hukoCrypto.createKeys();
						isKeyGenerated = true;
						System.out.println("Key Generator Executed");
					}
					else
						JOptionPane.showMessageDialog(Screen.this, "Key is already generated!");
				}
				else if(e.getSource() == Screen.this.menuItemConnect)
				{
					if(isKeyGenerated == true)
					{
						if(isNetworkConnectionEnabled == false)
						{
							while(username == null)
								username = JOptionPane.showInputDialog("Enter your name");
							isNetworkConnectionEnabled = true;
							
							//SENDS CONNECTION REQUEST TO THE PORT 6789
							try 
							{
								clientFriendSocket = new Socket("localhost", 6789); 
								postToServer = new DataOutputStream(clientFriendSocket.getOutputStream());
							    getFromServer = new BufferedReader(new InputStreamReader(clientFriendSocket.getInputStream()));
							 
								postToServer.writeBytes(username + '\n');
								chatScreen.setText("You have joined the chat!");
							}catch(Exception exception){exception.printStackTrace();}
							System.out.println("Connected to Network");
						}
						else
							JOptionPane.showMessageDialog(Screen.this, "You are already connected to network!");	
					}
					else
						JOptionPane.showMessageDialog(Screen.this, "You should Generate Key first!");
				}
				else if(e.getSource() == Screen.this.menuItemDisconnect)
				{
					if(isNetworkConnectionEnabled == true)
					{
						try{clientFriendSocket.close(); isNetworkConnectionEnabled = false;}catch(Exception exception){exception.printStackTrace();}
						chatScreen.setText("You have been disconnected!");
						onlineUserScreen.setText("");
						System.out.println("Disconnected from Network");
					}
					else
						JOptionPane.showMessageDialog(Screen.this, "You are already Disconnected!");
				}
				else if(e.getSource() == Screen.this.menuItemDeveloper)
					JOptionPane.showMessageDialog(Screen.this, "Hurkan Ugur\nYeditepe University\nComputer Engineering\n20160702051");
				else if(e.getSource() == Screen.this.menuItemExit)
				{
					System.out.println("Application Exit");
					System.exit(0);
				}
			}
		}
		@Override
		public void keyPressed(KeyEvent e) {}
		@Override
		public void keyTyped(KeyEvent e) {}
		@Override
		public void keyReleased(KeyEvent e)
		{
			if(e.getKeyCode() == KeyEvent.VK_ENTER)
			{
				//IF THE TEXTBOX IS NOT EMPTY
				if(messageTextBox.getText().trim().length() != 0 && isNetworkConnectionEnabled == true)
				{
					chatScreen.setText(chatScreen.getText() + "\nMe: " + messageTextBox.getText());
					//SEND MESSAGE TO THE SERVER
					try 
					{
						//SEND YOUR PUBLIC KEY TO EVERYONE AGAIN [IN CASE OF PACKAGE LOSS, SEND YOUR KEYS EVERY TIME !!!]
						postToServer.writeBytes("$$$" + hukoCrypto.ConvertPublicKeyToString(hukoCrypto.getPublicKey()) + '\n');
						
						if(messageTextBox.getText().startsWith("PM"))
						{
							String ToWho = messageTextBox.getText().substring(3);
							ToWho = ToWho.substring(0, ToWho.indexOf(" "));
							String privateMessage = ((messageTextBox.getText().substring(3)).substring((messageTextBox.getText().substring(3)).indexOf(" ")+1));
							String cipherText = hukoCrypto.encryptText(privateMessage, hukoCrypto.getPrivateKey());
							postToServer.writeBytes("[PRIVATE] " + ToWho + " " + cipherText + '\n');
						}
						else
						{
							//ENCRYPT MESSAGES WITH YOUR OWN PRIVATE KEY
							String cipherText = hukoCrypto.encryptText(messageTextBox.getText(), hukoCrypto.getPrivateKey());
							postToServer.writeBytes(cipherText + '\n');
						}
					} 
					catch (Exception exception) {exception.printStackTrace();}
					messageTextBox.setText("");
				}
			}	
		}
	}
	private static class RSACryptography
	{
		private volatile KeyPairGenerator keyGen;
	    private volatile KeyPair pair;
	    private volatile PrivateKey privateKey;
	    private volatile PublicKey publicKey;
	    private volatile Cipher cipher;
	    public RSACryptography()
	    {
	    	try 
	    	{
	    		this.keyGen = KeyPairGenerator.getInstance("RSA");
		        this.keyGen.initialize(2048);
		        this.cipher = Cipher.getInstance("RSA");
	    	}
	    	catch(Exception e) {e.printStackTrace();}
	    }
	    public PrivateKey getPrivateKey() {
	        return this.privateKey;
	    }
	    public PublicKey getPublicKey() {
	        return this.publicKey;
	    }
	    public void createKeys() {
	        this.pair = this.keyGen.generateKeyPair();
	        this.privateKey = pair.getPrivate();
	        this.publicKey = pair.getPublic();
	    }
	    public PrivateKey getPrivateKeyFromString(String stringKey)
	    {
	    	try
	    	{
	    		byte[] privateBytes = Base64.getDecoder().decode(stringKey.getBytes());
		        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateBytes);
		        KeyFactory keyFactor = KeyFactory.getInstance("RSA");
		        return keyFactor.generatePrivate(keySpec);
	    	}
	    	catch(Exception e)
	    	{
	    		System.out.println("Error: getPrivateKeyFromString()");
	    		return null;
	    	}
	    }
	   
	    public PublicKey getPublicKeyFromString(String stringKey)
	    {
	    	try 
	    	{
	    		byte[] publicBytes = Base64.getDecoder().decode(stringKey.getBytes());
		        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
		        KeyFactory keyFactor = KeyFactory.getInstance("RSA");
		        return keyFactor.generatePublic(keySpec);
	    	}
	    	catch(Exception e)
	    	{
	    		System.out.println("Error: getPublicKeyFromString()");
	    		return null;
	    	}
	    }
	    public String ConvertPublicKeyToString(PublicKey publicKey)
	    {
	    	return Base64.getEncoder().encodeToString(publicKey.getEncoded());
	    }
	    public String ConvertPrivateKeyToString(PrivateKey privateKey)
	    {
	    	return Base64.getEncoder().encodeToString(privateKey.getEncoded());
	    }
	    public String encryptText(String outgoingMessage, PrivateKey privateKey)
	    {
	    	try 
	    	{
		        this.cipher.init(Cipher.ENCRYPT_MODE, privateKey);
		        cipher.update(outgoingMessage.getBytes(StandardCharsets.US_ASCII));
		        return Base64.getEncoder().encodeToString(cipher.doFinal());
		    }
	    	catch(Exception e) {return null;}
	    }
	    public String decryptText(String incomingMessage, PublicKey publicKey)
	    {
	    	try 
	    	{
		        this.cipher.init(Cipher.DECRYPT_MODE, publicKey);
		        return new String(cipher.doFinal(Base64.getDecoder().decode(incomingMessage.getBytes(StandardCharsets.US_ASCII))), StandardCharsets.US_ASCII);
	    	}
	    	catch(Exception e) {return null;}
	    }
	}
}
