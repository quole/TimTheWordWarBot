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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.*;

/**
 *
 * @author Matthew Walker
 */
class ServerListener extends ListenerAdapter {
	@Override
	public void onConnect(ConnectEvent event) {
		String post_identify = Tim.db.getSetting("post_identify");
		if (!"".equals(post_identify)) {
			event.respond(post_identify);
		}

		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Tim.warticker = WarTicker.getInstance();
		Tim.deidler = DeIdler.getInstance();

		if (!Tim.db.getSetting("twitter_access_key").equals("")) {
			Tim.twitterstream = new TwitterIntegration();
			Tim.twitterstream.startStream();
		}
	}

	@Override
	public void onKick(KickEvent event) {
		if (event.getRecipient().getNick().equals(Tim.bot.getNick())) {
			Tim.db.deleteChannel(event.getChannel());
		}
	}

	@Override
	public void onInvite(InviteEvent event) {
		if (!Tim.db.ignore_list.contains(event.getUser())) {
			Tim.bot.sendIRC().joinChannel(event.getChannel());
			if (!Tim.db.channel_data.containsKey(event.getChannel())) {
				Tim.db.joinChannel(Tim.bot.getUserChannelDao().getChannel(event.getChannel()));
			}
		}
	}

	@Override
	public void onJoin(JoinEvent event) {
		if (!event.getUser().getNick().equals(Tim.bot.getNick())) {
			ChannelInfo cdata = Tim.db.channel_data.get(event.getChannel().getName().toLowerCase());
			int warscount = 0;

			try {
				String message = "";
				if (cdata.chatter_enabled.get("silly_reactions")
					&& !Tim.db.ignore_list.contains(event.getUser().getNick().toLowerCase())
					&& !Tim.db.soft_ignore_list.contains(event.getUser().getNick().toLowerCase())
				) {
					message = String.format(Tim.db.greetings.get(Tim.rand.nextInt(Tim.db.greetings.size())), event.getUser().getNick());
				}

				if (cdata.chatter_enabled.get("helpful_reactions")
					&& Tim.warticker.wars.size() > 0
					&& !Tim.db.ignore_list.contains(event.getUser().getNick().toLowerCase())
				) {
					warscount = Tim.warticker.wars.entrySet().stream().filter((wm) -> (wm.getValue().getChannel().equals(event.getChannel()))).map((_item) -> 1).reduce(warscount, Integer::sum);

					if (warscount > 0) {
						boolean plural = warscount >= 2;
						if (!message.equals("")) {
							message += " ";
						}

						message += "There " + (plural ? "are" : "is") + " " + warscount + " war" + (plural ? "s" : "")
							+ " currently running in this channel:";
					}
				}

				Thread.sleep(500);
				if (!message.equals("")) {
					event.getChannel().send().message(message);
				}

				if (cdata.chatter_enabled.get("helpful_reactions")
					&& warscount > 0
					&& !Tim.db.ignore_list.contains(event.getUser().getNick().toLowerCase())
				) {
					Tim.warticker.wars.entrySet().stream().filter((wm) -> (wm.getValue().getChannel().equals(event.getChannel()))).forEach(
						(wm) -> event.getChannel().send().message(wm.getValue().getDescription())
					);
				}

				if (cdata.chatter_enabled.get("helpful_reactions") && (Pattern.matches("(?i)mib_......", event.getUser().getNick()) || Pattern.matches("(?i)guest.*", event.getUser().getNick()))) {
					Thread.sleep(500);
					event.getChannel().send().message(
						String.format("%s: To change your name type the following, putting the name you want instead of NewNameHere: /nick NewNameHere", event.getUser().getNick()));
				}

				int r = Tim.rand.nextInt(100);

				if (cdata.chatter_enabled.get("silly_reactions")
					&& !Tim.db.ignore_list.contains(event.getUser().getNick().toLowerCase())
					&& !Tim.db.soft_ignore_list.contains(event.getUser().getNick().toLowerCase())
					&& r < 15
				) {
					Thread.sleep(500);
					if (Tim.rand.nextBoolean()) {
						r = Tim.rand.nextInt(Tim.db.extra_greetings.size());
						event.getChannel().send().message(Tim.db.extra_greetings.get(r));
					} else {
						int velociraptorCount = cdata.activeVelociraptors;
						String velociraptorDate = cdata.getLastSighting();

						event.getChannel().send().message(String.format("This channel has %d active velociraptors! The last one was spotted on %s.", velociraptorCount, velociraptorDate));
					}
				}

				if (cdata.chatter_enabled.get("silly_reactions") && event.getUser().getNick().toLowerCase().equals("trillian")) {
					Thread.sleep(1000);
					event.getChannel().send().message("All hail the velociraptor queen!");
					Tim.raptors.sighting(event);
				}
			} catch (InterruptedException ex) {
				Logger.getLogger(ServerListener.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}
