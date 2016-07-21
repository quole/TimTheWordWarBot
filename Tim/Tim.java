package Tim;

/*
 * Copyright (C) 2015 mwalker
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pircbotx.Configuration;
import org.pircbotx.Configuration.Builder;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;

public class Tim {
	public static Amusement amusement;
	public static PircBotX bot;
	public static Challenge challenge;
	public static AppConfig config = AppConfig.getInstance();
	public static DBAccess db = DBAccess.getInstance();
	public static Tim instance;
	public static MarkovChains markov;
	public static Random rand;
	public static ChainStory story;
	public static WarTicker warticker;
	public static DeIdler deidler;
	public static TwitterIntegration twitterstream;
	public static VelociraptorHandler raptors;

	public static void main(String[] args) {
		instance = new Tim();
	}

	public Tim() {
		rand = new Random();
		story = new ChainStory();
		challenge = new Challenge();
		markov = new MarkovChains();
		amusement = new Amusement();
		raptors = new VelociraptorHandler();

		Builder configBuilder = new Configuration.Builder()
			.setName(db.getSetting("nickname"))
			.setLogin("WarMech")
			.setNickservPassword(db.getSetting("password"))
			.addListener(new AdminCommandListener())
			.addListener(new UserCommandListener())
			.addListener(new ReactionListener())
			.addListener(new ServerListener())
			.setServerHostname(db.getSetting("server"))
			.setServerPassword(db.getSetting("server_password"))
			.setEncoding(Charset.forName("UTF-8"))
			.setMessageDelay(Long.parseLong(db.getSetting("max_rate")))
			.setAutoNickChange(true);

		db.refreshDbLists();

		// Join our channels
		db.channel_data.entrySet().forEach((entry) -> configBuilder.addAutoJoinChannel(entry.getValue().channel));

		bot = new PircBotX(configBuilder.buildConfiguration());

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (Tim.bot.isConnected()) {
					try {
						Tim.shutdown();
						Thread.sleep(1000);
					} catch (InterruptedException ex) {
						Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}
		});

		try {
			bot.startBot();
		} catch (IrcException | IOException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public static void shutdown() {
		if (Tim.bot.isConnected()) {
			Tim.bot.stopBotReconnect();
			Tim.bot.sendIRC().quitServer("HELP! Utoxin just murdered me! (Again!!!)");
			Tim.warticker.warTicker.cancel();
			Tim.deidler.idleTicker.cancel();
			Tim.twitterstream.userStream.shutdown();
			Tim.twitterstream.publicStream.shutdown();
		}
	}

	/**
	 * Singleton access method.
	 *
	 * @return Singleton
	 */
	public static Tim getInstance() {
		return instance;
	}
}
