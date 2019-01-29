package com.google.api.services.samples.youtube.cmdline.data;/*
 * Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.samples.youtube.cmdline.Auth;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.google.common.collect.Lists;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Prints a list of videos based on a search term.
 *
 * @author Jeremy Walker
 */
public class ChannelVideo {

    private static String PROPERTIES_FILENAME = "youtube.properties";
    //private static long NUMBER_OF_VIDEOS_RETURNED;
    public static YouTube youtube;

    /**
     * Initializes YouTube object to search for videos on YouTube (Youtube.com.google.api.services.samples.youtube.cmdline.data.Search.List). The program
     * then prints the names and thumbnails of each of the videos (only first 50 videos).
     *
     * @param args command line args.
     */
    private static List<String> ids;
    public static Map<String, Map<String, String>> data;
    public static List<SearchResult> searchResultList;
    public static String queryTerm;
    public static String apiKey;

    public ChannelVideo() throws IOException {

        // create_XLS();
        //Считывает api ключ пользователя
        Properties properties = new Properties();
        Map <String,List<PlaylistItem>> data = new HashMap<String, List<PlaylistItem>>();
        List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.force-ssl");

        try {
            InputStream in = Search.class.getResourceAsStream("/" + PROPERTIES_FILENAME);
            properties.load(in);
        } catch (IOException e) {
            System.err.println("There was an error reading " + PROPERTIES_FILENAME + ": " + e.getCause()
                    + " : " + e.getMessage());
            System.exit(1);
        }

        try {
            /*
             * The YouTube object is used to make all API requests. The last argument is required, but
             * because we don't need anything initialized when the HttpRequest is initialized, we override
             * the interface and provide a no-op function.
             */
            Credential credential = Auth.authorize(scopes, "commentthreads");
            youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential).setApplicationName("youtube-cmdline-search-sample").build();
            //заносим настройки в запрос - API ключ
            apiKey = properties.getProperty("youtube.apikey");
            //здесь 6 каналов, в данный момент 1
            data.put("UCRokSp8CGOuQO4R0F1RxRGg",getVideos("UCRokSp8CGOuQO4R0F1RxRGg"));
            System.out.println("end");


        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

    /*
     * Returns a query term (String) from user via the terminal.
     */
    private static List<PlaylistItem> getVideos(String id) throws IOException {


        YouTube.Channels.List channels = youtube.channels().list("id,snippet,contentDetails");
        channels.setKey(apiKey);
        channels.setId(id);
        List<PlaylistItem> playlistItemList = new ArrayList<PlaylistItem>();
        //отправляем запрос на сервер
        ChannelListResponse searchResponse = channels.execute();

        List<Channel> list = searchResponse.getItems();
        Channel channel = list.get(0);
        Object c = channel.get("contentDetails");

        ObjectMapper oMapper = new ObjectMapper();
        Map<String, Object> map = oMapper.convertValue(c, Map.class);

        Object b = map.get("relatedPlaylists");
        Map<String, Object> map2 = oMapper.convertValue(b, Map.class);
        String upl_id = map2.get("uploads").toString();
        YouTube.PlaylistItems.List playlistItemRequest =
                youtube.playlistItems().list("id,contentDetails,snippet");

        playlistItemRequest.setPlaylistId(upl_id);
        playlistItemRequest.setMaxResults((long)50);
        playlistItemRequest.setFields(
                "items(contentDetails/videoId,snippet/title,snippet/publishedAt),nextPageToken,pageInfo");

        String nextToken = "";
        int count=0;
        do {
            playlistItemRequest.setPageToken(nextToken);
            PlaylistItemListResponse playlistItemResult = playlistItemRequest.execute();
            playlistItemList.addAll(playlistItemResult.getItems());
            nextToken = playlistItemResult.getNextPageToken();
            count+=playlistItemResult.getItems().size();
        } while (nextToken != null);

        System.out.println("всего видео у данного канала:"+count);
        return playlistItemList;
    }

}
