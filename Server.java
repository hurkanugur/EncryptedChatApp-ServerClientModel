import java.net.*;
import java.util.ArrayList;
import java.io.*;
public class Server 
{
	public static void main(String args[]) throws Exception
	{	
		ArrayList<Socket> clientFriendsSocketList = new ArrayList<>();
		ArrayList<String> clientFriendsList = new ArrayList<>();
		ServerSocket whatsappServerSocket = new ServerSocket(6789);
		while (true) 
		{
			//WAIT UNTIL SOMEONE JOINS THE GROUP CHAT
			Socket newClientFriendSocket = whatsappServerSocket.accept();

			//WHEN HE ENTERS THE NICKNAME IN TCPClient.java, READ IT AND SAVE IT
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(newClientFriendSocket.getInputStream()));
			String clientFriendName = bufferedReader.readLine();
			
			//SAVE THE NEW CHAT MEMBERS NAME AND SOCKET IN ARRAYLISTS
			clientFriendsList.add(clientFriendName);
			clientFriendsSocketList.add(newClientFriendSocket);

			//OUR NEW FRIEND IS READY TO TALK WITH US YEEEY !!!
			new Thread(new HurkanUgurWhatsappServer(newClientFriendSocket, clientFriendsSocketList, clientFriendName, clientFriendsList)).start();
		}
	}
}
class HurkanUgurWhatsappServer extends Thread 
{
	private ArrayList<Socket> socketFriendsList;
	private ArrayList<String> clientFriendsList;
	private Socket currentFriendSocket;
	private String currentFriendName;
	private String currentFriendsText;
	
	public HurkanUgurWhatsappServer(Socket currentFriendSocket, ArrayList<Socket> socketFriendsList, String currentFriendName, ArrayList<String> clientFriendsList)
	{
		this.currentFriendSocket = currentFriendSocket;
		this.socketFriendsList = socketFriendsList;
		this.currentFriendName = currentFriendName;
		this.clientFriendsList = clientFriendsList;
	}
	@Override
	public void run() 
	{
		try 
		{
			System.out.println("Incoming connection from->> " + currentFriendSocket.getRemoteSocketAddress());
			System.out.println("##CONN->> " + currentFriendName);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(currentFriendSocket.getInputStream()));

			//PRINT ALL NEW CONNECTIONS OTHER THAN YOURSELF
			for (Socket socket : socketFriendsList) 
			{
				//NOTIFY ONLY YOUR FRIENDS THAT YOU HAVE JOINED THE GROUP (DO NOT WRITE THIS FOR YOURSELF)
				if (socket != currentFriendSocket) 
				{
					DataOutputStream toAllFriendClients = new DataOutputStream(socket.getOutputStream());
					toAllFriendClients.writeBytes(currentFriendName + " has connected!" + '\n');
				}
			}

			//PRINT THE ALL MEMBERS OF THE GROUP CHAT LIKE [HURKAN, UGUR, HARRY, POTTER]
			for (Socket socket : socketFriendsList) 
			{
				DataOutputStream toAllFriendClients = new DataOutputStream(socket.getOutputStream());
				toAllFriendClients.writeBytes("Online Group Members: " + clientFriendsList.toString() + '\n');
			}
			
			while (true) 
			{
				DataOutputStream dataOutputStream = null;
				currentFriendsText = bufferedReader.readLine();

				try 
				{
					System.out.println(currentFriendName + "->> " + currentFriendsText);
	
					//IF IT IS A NORMAL MESSAGE [NEITHER "PM" NOR "LIST"], WRITE EVERYONE'S CONSOLE WHAT CURRENT PEOPLE WROTE
					if (!currentFriendsText.equalsIgnoreCase("list")) 
					{
						for (Socket socket : socketFriendsList) 
						{
							//BUT DO NOT WRITE IT THE SENDER'S OWN CONSOLE
							if(socket != currentFriendSocket) 
							{
								dataOutputStream = new DataOutputStream(socket.getOutputStream());
								dataOutputStream.writeBytes(currentFriendName + ": " + currentFriendsText + '\n');
							}
						}
					}
				}catch(Exception e)
				{
					throw new Exception();
				}
			}
		}
		//CLIENT LEAVES THE SERVER
		//WHEN A CLIENT LEAVES THE CHAT, IT WILL THROW AN EXCEPTION
		catch (Exception hukoException) 
		{
			//WRITE THE SERVER-SIDE CHAT WHO IS DISCONNECTED
			System.out.println("##DISCON->> " + currentFriendName);
			
			//WRITE EVERYONE'S CONSOLE THAT X PERSON HAS LEFT THE CHAT
			for(Socket socket: socketFriendsList)
			{
				try
				{
					DataOutputStream toAllFriendClients = new DataOutputStream(socket.getOutputStream());
					toAllFriendClients.writeBytes(currentFriendName +" has disconnected!"+'\n');
				}
				catch(IOException exception){}
			}
			
			//DELETE DISCONNECTED USER FROM THE LIST
			socketFriendsList.remove(currentFriendSocket);
			clientFriendsList.remove(currentFriendName);
			try {currentFriendSocket.close();} catch (Exception e) {e.printStackTrace();}
		}
	}
}