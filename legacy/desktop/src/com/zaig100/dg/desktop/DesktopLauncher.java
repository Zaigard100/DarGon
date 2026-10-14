package com.zaig100.dg.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.zaig100.dg.Main;
import com.zaig100.dg.utils.Configuration;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;

public class DesktopLauncher {
	static final String[] spl = new String[] {

			"First","Lapenko Forever", "Subscribe to the grup in VK","USSR","Russia",
			"You know","DOOOOOOOOOGY","OPG 'Iron Sleeves' ","Diamonds aren't the best","Mamix",
			"FUGA TV","5opka","We are on YT","Hi","X% bugs","Написано на русском",
			"Written in English","Albert the cat","2021","DAZ","AZD","ZAD","ADZ","Without ads.",
			"I advertise myself","No lag, just frizes","Made by Zaigard100","Netzus is here...", "Are you deluded?",
			"Also try Prelude of the Chambered","Java Edition","Stop how to stop writing splashes?",
			"V^V^V^V^V^V^V^V^V^V^V^V^V^V^V^V^V^V","Welcome to the dungeon DarGon",
			"Another splash","Very Well!!!!","And you are subscribed to groups?","They are already here...",
			"Globglabgala","System.out.println('Hello World!!');","frame = new Sprite(fram.getColorBufferTexture());",
			"Remember 0.1beta1","Pay attention to 1 and 14","Also try Portallers 2D","XlebMedovi","Yes..","Open the chest"
			,"Give money for food","Perhaps the original","Braid in 4K","Bonsai Garden","Last"

	};
	static final String[] icon = new String[]{"texture/icon.png","texture/amonghero.png","texture/firedhero.png","texture/shotedhero.png"};
	public static void main (String[] args) throws IOException, InterruptedException {

		System.out.println("DarGon start");

		GregorianCalendar calendar = new GregorianCalendar();
		Random r = new Random();
		Main m = new Main(false);

		Configuration.load(m);

		String title = "DarGon: " + spl[r.nextInt(spl.length)];


		if((calendar.get(Calendar.MONTH)== Calendar.JUNE)&&(calendar.get(Calendar.DAY_OF_MONTH)==18)) {
			title = "DarGon: " + "Today may be the developer's birthday";
		}

		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

		config.vSyncEnabled = true;
		config.foregroundFPS = 10000;
		config.title = title;
		config.width = (int) (7 *16*Configuration.getScale());
		config.height = (int) (5 *16*Configuration.getScale());
		config.resizable = false;
		config.addIcon(icon[r.nextInt(icon.length)], Files.FileType.Internal);

		if(config.width<(int) (7 *16*Configuration.getScale())){
			config.width = (int) (7 *16*Configuration.getScale());
		}
		if(config.height<(int) (5 *16*Configuration.getScale())) {
			config.height = (int) (5 * 16 * Configuration.getScale());
		}

		for(int i = 0; i < args.length;i++){
			switch (args[i]){
				case "-r": // -r 896 640
					try {
						config.width = Integer.parseInt(args[i + 1]);
						config.height = Integer.parseInt(args[i + 2]);
					}catch (Exception e){
						System.out.println("Invalid \"-resolution\" argument");
					}
					break;
				case "-fs": // -fs true
					try{
					config.fullscreen = Boolean.parseBoolean(args[i+1]);
					}catch (Exception e){
						System.out.println("Invalid \"-fullscreen\" argument");
					}
					break;
				case "-scale": // -scale 8
					try{
					Configuration.setScale(Integer.parseInt(args[i+1]));
					}catch (Exception e){
						System.out.println("Invalid \"-scale\" argument");
					}
					break;
				case "-fps": // -fps 30
					config.foregroundFPS = Integer.parseInt(args[i+1]);
					break;
				case "-vsync": // -vsync false
					config.vSyncEnabled = Boolean.parseBoolean(args[i+1]);
					break;
			}
		}

		new LwjglApplication(m, config);
	}
}
