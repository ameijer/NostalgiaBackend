package com.nostalgia.persistence.model;

import java.util.*;

import org.geojson.Point;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.*;

import java.io.Serializable;

/**
 * Created by alex on 11/4/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements Serializable {

	@JsonIgnore
	public String getChannelName() {
		return _id.substring(0, 8);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 3672636185090518520L;

	private String type = this.getClass().getSimpleName();
	private long versionNumber;
	private String _id = UUID.randomUUID().toString();

	private String seenVideosPtr = UUID.randomUUID().toString();
	private String username;

	private String passwordPtr;

	private String homeRegion = "us_east";

	// list of channels user has access to
	private List<String> admin_channels;
	private List<String> admin_roles;

	private HashMap<String, String> locationHistory;

	private Map<String, String> streamTokens;

	// channel -> time
	private Map<String, String> video_channels;

	// channels that this document itself is in
	private List<String> channels;

	private boolean disabled = false;

	private String email;

	private Point focusedLocation;

	private Point lastKnownLoc;
	private long lastLocationUpdate;

	private long dateJoined;
	private long lastSeen;

	private Map<String, String> collections;

	private Set<String> location_channels;

	private List<Account> accountsList;

	private HashSet<String> user_channels;

	private HashMap<String, String> friends;
	private HashMap<String, String> pendingFriends;
	private Map<String, String> settings;

	private boolean emailVerified;

	private Map<String, String> accounts;

	// locId -> time
	private Map<String, String> userLocations;
	private String icon;

	private List<String> authorizedDevices;
	private String token;

	private String syncToken;

	private List<String> createdLocations;

	private HashSet<String> silentSubscriptions;

	// these are updated using atomic prepend
	private String upvoteTrackerId = UUID.randomUUID().toString();

	public Map<String, String> getVideo_channels() {
		return video_channels;
	}

	public void setVideo_channels(Map<String, String> video_channels) {
		this.video_channels = video_channels;
	}

	@JsonIgnore
	public synchronized HashSet<String> purgeOlderThan(long unixTimeStamp) {
		if (video_channels == null)
			return null;
		HashSet<String> removed = new HashSet<String>();
		for (String id : video_channels.keySet()) {
			if (Long.parseLong(video_channels.get(id)) < unixTimeStamp) {
				// purge
				this.video_channels.remove(id);
				admin_channels.remove(id);
				removed.add(id);
			}
		}
		return removed;
	}

	public boolean isEmailVerified() {
		return emailVerified;
	}

	public Set<String> getLocation_channels() {
		return location_channels;
	}

	public void setLocation_channels(Set<String> location_channels) {
		this.location_channels = location_channels;
	}

	public HashSet<String> getUser_channels() {
		return user_channels;
	}

	public void setUser_channels(HashSet<String> user_channels) {
		this.user_channels = user_channels;
	}

	public void setEmailVerified(boolean emailVerified) {
		this.emailVerified = emailVerified;
	}

	@JsonIgnore
	public Set<String> silentSubscribeToLocations(Collection<KnownLocation> values) {

		for (KnownLocation toSubTo : values) {
			if (!silentSubscriptions.contains(toSubTo.get_id())) {
				// add to set
				silentSubscriptions.add(toSubTo.get_id());

				// add to admin chanells
				admin_channels.add(toSubTo.getChannelName());

			}
		}

		return silentSubscriptions;
	}

	@JsonIgnore
	public synchronized Map<String, String> updateVideoChannels(Set<String> videosToSubscribeTo) {
		// clear old locations out from subscriptions
		// all the locations we subscribe to

		if (this.video_channels == null) {
			this.video_channels = new HashMap<String, String>();
		}

		if (admin_channels == null) {
			admin_channels = new ArrayList<String>();
		}

		for (String exists : this.video_channels.keySet()) {

			if (videosToSubscribeTo.contains(exists)) {
				// then we were already here. remove it from the list
				videosToSubscribeTo.remove(exists);
			}
			// } else {
			//

			// }

		}

		for (String vid : videosToSubscribeTo) {
			this.video_channels.put(vid, System.currentTimeMillis() + "");
			admin_channels.add(vid);
		}

		return this.video_channels;

	}

	@JsonIgnore
	public synchronized Set<String> updateLocationChannels(HashMap<String, KnownLocation> nearbys) {
		// clear old locations out from subscriptions
		// all the locations we subscribe to

		if (this.location_channels == null) {
			this.location_channels = new HashSet<String>();
		}

		if (admin_channels == null) {
			admin_channels = new ArrayList<String>();
		}

		if (locationHistory == null) {
			locationHistory = new HashMap<String, String>();
		}

		for (Iterator<String> it = location_channels.iterator(); it.hasNext();) {

			String exists = it.next();
			String channel = exists.substring(0, 8);
			if (nearbys.keySet().contains(channel)) {
				// then we were already here.
				continue;
			} else {

				it.remove();
				admin_channels.remove(channel);
			}

		}

		// now, all nearbys has left are new points that arent in existing
		// and exisitng has only the points it has in common with nearbys

		// finally, add in all the nearby points we arent subscribed to yet

		for (KnownLocation loc : nearbys.values()) {
			if (!this.location_channels.contains(loc.get_id())) {
				this.location_channels.add(loc.get_id());
				admin_channels.add(loc.getChannelName());
			}
		}

		// update history
		for (KnownLocation loc : nearbys.values()) {
			locationHistory.put(loc.get_id(), Long.toString(System.currentTimeMillis()));
		}

		return this.location_channels;

	}

	@JsonIgnore
	public synchronized HashSet<String> subscribeToUserChannel(String channelName) {
		// clear old locations out from subscriptions
		// all the locations we subscribe to

		if (this.user_channels == null) {
			this.user_channels = new HashSet<String>();
		}

		if (admin_channels == null) {
			admin_channels = new ArrayList<String>();
		}

		if (this.user_channels.contains(channelName)) {
			return this.user_channels;
		} else {
			this.user_channels.add(channelName);
			admin_channels.add(channelName);
		}

		return this.user_channels;

	}

	@JsonIgnore
	public synchronized HashSet<String> unsubscribeFromUserChannel(String channelName) {
		// clear old locations out from subscriptions
		// all the locations we subscribe to
		HashSet<String> existing = this.user_channels;
		if (existing == null) {
			existing = new HashSet<String>();
		}

		if (admin_channels == null) {
			admin_channels = new ArrayList<String>();
		}

		if (existing.contains(channelName)) {
			existing.remove(channelName);
			admin_channels.remove(channelName);
		}

		return existing;

	}

	public String getSyncToken() {
		return syncToken;
	}

	public Map<String, String> getAccounts() {
		return accounts;
	}

	public void setAccounts(Map<String, String> accounts) {
		this.accounts = accounts;
	}

	public List<String> getAuthorizedDevices() {
		return authorizedDevices;
	}

	public void setAuthorizedDevices(ArrayList<String> arrayList) {
		this.authorizedDevices = arrayList;
	}

	public User() {
		if (this.userLocations == null) {
			userLocations = new HashMap<String, String>();
		}

		if (this.locationHistory == null) {
			locationHistory = new HashMap<String, String>();
		}

		if (this.collections == null) {
			collections = new HashMap<String, String>();
		}

		if (this.accountsList == null) {
			accountsList = new ArrayList<Account>();
		}
		if (this.createdLocations == null) {
			createdLocations = new ArrayList<String>();
		}
		if (friends == null) {
			friends = new HashMap<String, String>();
		}
		if (pendingFriends == null) {
			pendingFriends = new HashMap<String, String>();
		}

		if (this.silentSubscriptions == null) {
			this.silentSubscriptions = new HashSet<String>();
		}
	}

	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String name) {
		this.username = name;
	}

	public long getDateJoined() {
		return dateJoined;
	}

	public void setDateJoined(long dateJoined) {
		this.dateJoined = dateJoined;
	}

	public Map<String, String> getFriends() {
		return friends;
	}

	public void setFriends(HashMap<String, String> friends) {
		this.friends = friends;
	}

	public Map<String, String> getSettings() {
		return settings;
	}

	public void setSettings(Map<String, String> settings) {
		this.settings = settings;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public long getLastSeen() {
		return lastSeen;
	}

	public void setLastSeen(long lastSeen) {
		this.lastSeen = lastSeen;
	}

	public Point getLastKnownLoc() {
		return lastKnownLoc;
	}

	public void setLastKnownLoc(Point lastKnownLoc) {
		this.lastKnownLoc = lastKnownLoc;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getToken() {
		return token;
	}

	public void setSyncToken(String session) {
		this.syncToken = session;

	}

	public List<String> getAdmin_roles() {
		return admin_roles;
	}

	public void setAdmin_roles(List<String> admin_roles) {
		this.admin_roles = admin_roles;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public List<String> getAdmin_channels() {
		return admin_channels;
	}

	public void setAdmin_channels(List<String> admin_channels) {
		this.admin_channels = admin_channels;
	}

	public String getPasswordPtr() {
		return passwordPtr;
	}

	public void setPasswordPtr(String password) {
		this.passwordPtr = password;
	}

	public List<String> getChannels() {
		return channels;
	}

	public void setChannels(List<String> channels) {
		this.channels = channels;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getHomeRegion() {
		return homeRegion;
	}

	public void setHomeRegion(String homeRegion) {
		this.homeRegion = homeRegion;
	}

	public long getLastLocationUpdate() {
		return lastLocationUpdate;
	}

	public void setLastLocationUpdate(long lastLocationUpdate) {
		this.lastLocationUpdate = lastLocationUpdate;
	}

	public Point getFocusedLocation() {
		return focusedLocation;
	}

	public void setFocusedLocation(Point focusedLocation) {
		this.focusedLocation = focusedLocation;
	}

	public Map<String, String> getStreamTokens() {
		return streamTokens;
	}

	public void setStreamTokens(Map<String, String> streamTokens) {
		this.streamTokens = streamTokens;
	}

	public Map<String, String> getUserLocations() {
		return userLocations;
	}

	public void setUserLocations(Map<String, String> userLocations) {
		this.userLocations = userLocations;
	}

	@JsonIgnore
	public synchronized Collection<String> subscribeToLocation(String loc_id) {
		// check for duplicate
		if (this.userLocations == null) {
			userLocations = new HashMap<String, String>();
		}

		if (userLocations.containsKey(loc_id)) {
			// no changes needed
			return userLocations.keySet();
		}

		// add in location + time it was added
		userLocations.put(loc_id, Long.toString(System.currentTimeMillis()));

		int end = loc_id.indexOf('-');
		String channelName = loc_id.substring(0, end);
		// add in channel ID to allow for subscriptions
		admin_channels.add(channelName);

		return userLocations.values();

	}

	public HashMap<String, String> getLocationHistory() {
		return locationHistory;
	}

	private void setLocationHistory(HashMap<String, String> history) {
		this.locationHistory = history;
	}

	public long getVersionNumber() {
		return versionNumber;
	}

	public void setVersionNumber(long versionNumber) {
		this.versionNumber = versionNumber;
	}

	public Collection<String> unsubscribeFromLocation(String idToRemove) {

		if (this.userLocations == null) {
			userLocations = new HashMap<String, String>();
		}

		String returned = userLocations.remove(idToRemove);
		if (returned == null) {
			return userLocations.values();
		}

		int end = idToRemove.indexOf('-');
		String channelName = idToRemove.substring(0, end);
		// add in channel ID to allow for subscriptions
		admin_channels.remove(channelName);

		return userLocations.values();

	}

	public Map<String, String> getCollections() {
		return collections;
	}

	public void setCollections(Map<String, String> collections) {
		this.collections = collections;
	}

	public List<Account> getAccountsList() {
		return accountsList;
	}

	public void setAccountsList(List<Account> accountsList) {
		this.accountsList = accountsList;
	}

	public static class Account {
		public String name;
		public String id;
		public String email;
	}

	public List<String> getCreatedLocations() {

		return createdLocations;
	}

	public void setCreatedLocations(List<String> createdLocations) {
		this.createdLocations = createdLocations;

	}

	public HashMap<String, String> getPendingFriends() {
		return pendingFriends;
	}

	public void setPendingFriends(HashMap<String, String> pendingFriends) {
		this.pendingFriends = pendingFriends;
	}

	@JsonIgnore
	public Map<String, String> subscribeToPendingFriend(User friendToAdd, boolean incomingRequest) {
		if (incomingRequest) {
			pendingFriends.put(friendToAdd.get_id(), "Received_" + Long.toString(System.currentTimeMillis()));
		} else {
			pendingFriends.put(friendToAdd.get_id(), "Sent_" + Long.toString(System.currentTimeMillis()));
		}
		return pendingFriends;
	}

	@JsonIgnore
	public Map<String, String> subscribeToFriend(User friendToAdd) {
		// TODO Auto-generated method stub
		friends.put(friendToAdd.get_id(), Long.toString(System.currentTimeMillis()));
		pendingFriends.remove(friendToAdd.get_id());

		if (!admin_channels.contains(friendToAdd.getChannelName())) {
			admin_channels.add(friendToAdd.getChannelName());
		}
		return friends;
	}

	@JsonIgnore
	public Map<String, String> denyFriend(User friendToRemove) {
		// TODO Auto-generated method stub
		String removed = pendingFriends.remove(friendToRemove.get_id());

		admin_channels.remove(friendToRemove.getChannelName());
		return pendingFriends;
	}

	@JsonIgnore
	public Map<String, String> unsubscribeFromFriend(User friendToRemove) {
		// TODO Auto-generated method stub
		String removed = friends.remove(friendToRemove.get_id());

		admin_channels.remove(friendToRemove.getChannelName());
		return friends;
	}

	// returns tags of all collections
	@JsonIgnore
	public Collection<String> addCollection(MediaCollection creating) throws Exception {

		if (creating.getName() == null || creating.getName().equals("")) {
			throw new IllegalArgumentException("name is required for storage in map");
		}

		JSONObject key = new JSONObject();
		JSONObject visibility = new JSONObject();
		visibility.put("visibility", creating.getVisibility());
		key.put("key", creating.getName());
		JSONArray ordered = new JSONArray();
		ordered.put(visibility);
		ordered.put(key);

		// check for existence
		String existing = collections.get(ordered.toString());

		if (existing != null) {
			throw new Exception("collection already exists!");
		}

		// add into collections
		collections.put(ordered.toString(), creating.get_id());

		// subscribe in channels
		admin_channels.add(creating.getChannelName());
		return collections.values();

	}

	@JsonIgnore
	public MediaCollection removeCollection(MediaCollection toRemove) {
		// check for existence
		if (toRemove.getName() == null || toRemove.getName().equals("")) {
			throw new IllegalArgumentException("name is required for removal from map");
		}

		JSONObject key = new JSONObject();
		JSONObject visibility = new JSONObject();
		visibility.put("visibility", toRemove.getVisibility());
		key.put("key", toRemove.getName());
		JSONArray ordered = new JSONArray();
		ordered.put(visibility);
		ordered.put(key);

		// check for existence
		String existing = collections.remove(ordered.toString());

		if (existing == null) {
			return null;
		}

		// remove from channels
		admin_channels.remove(toRemove.getChannelName());
		return toRemove;

	}

	@JsonIgnore
	public String getPublicVideoCollId() {
		JSONObject key = new JSONObject();
		JSONObject visibility = new JSONObject();
		visibility.put("visibility", MediaCollection.PUBLIC);
		key.put("key", this.get_id() + "_pub");
		JSONArray ordered = new JSONArray();
		ordered.put(visibility);
		ordered.put(key);

		String matching = collections.get(ordered.toString());
		return matching;
	}

	@JsonIgnore
	public String getPrivateVideoCollId() {

		JSONObject key = new JSONObject();
		JSONObject visibility = new JSONObject();
		visibility.put("visibility", MediaCollection.PRIVATE);
		key.put("key", this.get_id() + "_priv");
		JSONArray ordered = new JSONArray();
		ordered.put(visibility);
		ordered.put(key);
		String matching = collections.get(ordered.toString());
		return matching;
	}

	@JsonIgnore
	public String getSharedVideoCollId() {
		JSONObject key = new JSONObject();
		JSONObject visibility = new JSONObject();
		visibility.put("visibility", MediaCollection.SHARED);
		key.put("key", this.get_id() + "_shared");
		JSONArray ordered = new JSONArray();
		ordered.put(visibility);
		ordered.put(key);
		String matching = collections.get(ordered.toString());
		return matching;
	}

	@JsonIgnore
	public String findCollection(String visibility, String key) {

		JSONObject keyobj = new JSONObject();
		JSONObject visibilityobj = new JSONObject();
		visibilityobj.put("visibility", visibility);
		keyobj.put("key", key);
		JSONArray ordered = new JSONArray();
		ordered.put(visibilityobj);
		ordered.put(keyobj);
		String matching = collections.get(ordered.toString());
		return matching;

	}

	@JsonIgnore
	public List<String> findCollectionbyTag(String key) {
		String[] visibilities = new String[] { MediaCollection.PRIVATE, MediaCollection.PUBLIC,
				MediaCollection.SHARED };
		ArrayList<String> results = new ArrayList<String>();

		for (String visibility : visibilities) {
			JSONObject keyobj = new JSONObject();
			JSONObject visibilityobj = new JSONObject();
			visibilityobj.put("visibility", visibility);
			keyobj.put("key", key);
			JSONArray ordered = new JSONArray();
			ordered.put(visibilityobj);
			ordered.put(key);
			String matchingAtViz = collections.get(ordered.toString());
			if (matchingAtViz != null) {
				results.add(matchingAtViz);
			}
		}

		if (results.size() < 1)
			return null;
		return results;
	}

	public String getUpvoteTrackerId() {
		return upvoteTrackerId;
	}

	public void setUpvoteTrackerId(String upvoteTrackerId) {
		this.upvoteTrackerId = upvoteTrackerId;
	}

	public String getSeenVideosPtr() {
		return seenVideosPtr;
	}

	public void setSeenVideosPtr(String seenVideosPtr) {
		this.seenVideosPtr = seenVideosPtr;
	}

}
