package org.alopex.Condenser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import org.alopex.Condenser.sc.Playlist;
import org.alopex.Condenser.sc.SoundCloud;
import org.alopex.Condenser.sc.Track;
import org.alopex.Condenser.sc.User;

public class App {

	private static SoundCloud sc;
	private static ArrayList<Playlist> playlistPool;
	private static ArrayList<Track> favoritesPool;
	private static ArrayList<User> user;

	public static void main(String[] args) throws InterruptedException {
		System.out.println("Initializing Condenser v0.1");
		System.out.println();

		System.out.println("Creating SoundCloud authentication object...");
		sc = getSC();
		
		Scanner scan = new Scanner(System.in);
		System.out.print("Enter your SoundCloud username: ");
		String userName = scan.nextLine();
		
		System.out.println();
		System.out.println("User targeted: [" + userName + "]");
		
		System.out.println("Fetching ID...");
		user = sc.findUser(userName);
		
		if(user != null && user.size() > 0) {
			System.out.println("\tUser ID: [" + user.get(0).getId() + "]");
			
			System.out.println();
			System.out.println("Please enter which you would like to download (playlists | likes)");
			String nextInstruction = scan.nextLine();
			
			switch(nextInstruction) {
				case "playlists":
					pullPlaylists();
					break;
					
				case "likes":
					pullLikes();
					break;
					
				default:
					System.out.println("Could not recognize option: " + nextInstruction);
			}
		} else {
			System.out.println();
			System.out.println("No users were found under that username. Terminating...");
		}
		
		scan.close();
	}
	
	private static void pullPlaylists() {
		try {
			System.out.println();
			System.out.println("Pulling playlists...");
			playlistPool = sc.get("/users/" + user.get(0).getId() + "/playlists");

			System.out.println("Detected: " + playlistPool.size());
			System.out.println();

			for(int i = 0; i < playlistPool.size(); i++) {
				Playlist playlist = playlistPool.get(i);
				System.out.println("Downloading playlist [" + playlist.getTitle() + "]");
				String dirCheck = "CREATE";
				File playlistDir = new File(user.get(0).getUsername() + "/Playlists/" + escapeStringAsFilename(playlist.getTitle()));
				if(playlistDir.exists()) {
					dirCheck = "EXIST";
				} else {
					playlistDir.mkdirs();
				}
				System.out.println("Checking playlist directory: " + dirCheck + " | " + playlist.getTracks().size() + " tracks");
				ArrayList<Track> trackList = playlist.getTracks();
				for(int j = 0; j < trackList.size(); j++) {
					Track track = trackList.get(j);
					Thread pullThread = pullTrack(track, playlistDir);
					pullThread.start();
					
					Thread.sleep(2100);
					pullThread.interrupt();
				}
				Thread.sleep(4000);
				System.out.println();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static void pullLikes() {
		try {
			System.out.println();
			System.out.println("Pulling likes...");
			favoritesPool = sc.get("/users/" + user.get(0).getId() + "/favorites");
			
			String dirCheck = "CREATE";
			File favoritesDir = new File(user.get(0).getUsername() + "/Likes");
			if(favoritesDir.exists()) {
				dirCheck = "EXIST";
			} else {
				favoritesDir.mkdirs();
			}
			System.out.println("Checking favorites directory: " + dirCheck + " | " + favoritesPool.size() + " tracks");

			for(int i = 0; i < favoritesPool.size(); i++) {
				Track track = favoritesPool.get(i);
				
				Thread pullThread = pullTrack(track, favoritesDir);
				pullThread.start();
				Thread.sleep(3500);
				
				pullThread.interrupt();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static Thread pullTrack(final Track track, final File playlistDir) {
		return (new Thread(new Runnable() {
			public void run() {
				try {
					String streamUrlStr = track.getStreamUrl(sc);
					if(streamUrlStr != null) {
						try {
							URL streamURL = new URL(streamUrlStr);
							HttpURLConnection httpConn = (HttpURLConnection) streamURL.openConnection();
							File trackFile = new File(playlistDir.getPath() + "/" + escapeStringAsFilename(track.getTitle()) + ".mp3");
							
							if(trackFile.exists()) {
								System.out.println("\t[o] Track [" + track.getTitle() + "] skipped, already downloaded");
								return;
							} else {
								System.out.println("\t> Pulling track [" + track.getTitle() + "]");
								int responseCode = httpConn.getResponseCode();
								if(responseCode == HttpURLConnection.HTTP_OK) {
									InputStream inputStream = httpConn.getInputStream();
									FileOutputStream outputStream = new FileOutputStream(trackFile);

									int bytesRead = -1;
									byte[] buffer = new byte[4096];
									while ((bytesRead = inputStream.read(buffer)) != -1) {
										outputStream.write(buffer, 0, bytesRead);
									}
									outputStream.close();
									inputStream.close();
								} else {
									System.out.println("Download error. Server replied: " + responseCode);
								}
								httpConn.disconnect();
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					} else {
						System.out.println("\t[x] Track failed: " + track.getTitle() + ".mp3");
					}
				} catch (Exception ex) {
					System.out.println("\t===========================================");
					System.out.println("\tRate limiting occurred. Attempting reset...");
					System.out.println("\t===========================================");
					sc = getSC();
					pullTrack(track, playlistDir).start();
				}
			}
		}));
	}
	
	public static String escapeStringAsFilename(String in){
		return in.replaceAll("[:\\\\/*\"?|<>']", " ");
	}
	
	public static SoundCloud getSC() {
		return new SoundCloud("2213744dedb6eb7b7589af54254b5089", "fc5ccf215b53eaf6da7580390637ff82");
	}
}
