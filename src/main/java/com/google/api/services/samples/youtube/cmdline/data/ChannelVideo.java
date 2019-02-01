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
import org.apache.commons.collections4.list.TreeList;
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
    public static List <String> main_channels;
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
    public static Map <String,List<String>> video;
    public ChannelVideo() throws IOException {

        // create_XLS();
        //Считывает api ключ пользователя
        Properties properties = new Properties();
        apiKey = properties.getProperty("youtube.apikey");
        video = new HashMap<String, List<String>>();
        List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.force-ssl");
        main_channels=new TreeList<String>();
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
            main_channels.add("UCRokSp8CGOuQO4R0F1RxRGg");
           // main_channels.add("UCDBgSA_DZ0NNMtf2_C0zQXA");
            //main_channels.add("UCY_q9S5SOsIRZNUVrmHB1JQ");
            //main_channels.add("UC0oLxL8yFsI6KyXdDgnJi4g");
            //main_channels.add("UCN8NAFrJENowmi2f79gvCjA");
            //main_channels.add("UC2aSu7cxkw2-icfSrG0p1jg");
            //здесь 6 каналов, в данный момент 1
            //Белсат
            video.put("UCRokSp8CGOuQO4R0F1RxRGg",getVideo("UCRokSp8CGOuQO4R0F1RxRGg"));
            //Гарантий нет
           // video.put("UCDBgSA_DZ0NNMtf2_C0zQXA",getVideo("UCDBgSA_DZ0NNMtf2_C0zQXA"));
            //Nexta
            //video.put("UCY_q9S5SOsIRZNUVrmHB1JQ",getVideo("UCY_q9S5SOsIRZNUVrmHB1JQ"));
            //Народный репортер
            //video.put("UC0oLxL8yFsI6KyXdDgnJi4g",getVideo("UC0oLxL8yFsI6KyXdDgnJi4g"));
            //Покиньте вагон
            //video.put("UCN8NAFrJENowmi2f79gvCjA",getVideo("UCN8NAFrJENowmi2f79gvCjA"));
            //Паказуха
            //video.put("UC2aSu7cxkw2-icfSrG0p1jg",getVideo("UC2aSu7cxkw2-icfSrG0p1jg"));;


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
    private static List<String> getVideo(String id) throws IOException {


        YouTube.Channels.List channels = youtube.channels().list("id,snippet,contentDetails");
        channels.setKey(apiKey);
        channels.setId(id);
        List<PlaylistItem> playlistItemList = new ArrayList<PlaylistItem>();
        List<String> idList = new ArrayList<String>();
        //отправляем запрос на сервер
        ChannelListResponse searchResponse = channels.execute();

        List<Channel> items = searchResponse.getItems();
        Channel channel = items.get(0);
        Object contentDetails = channel.get("contentDetails");

        ObjectMapper oMapper = new ObjectMapper();
        Map<String, Object> map = oMapper.convertValue(contentDetails, Map.class);

        Object relatedPlaylists = map.get("relatedPlaylists");
        Map<String, Object> uploads = oMapper.convertValue(relatedPlaylists, Map.class);
        String uploads_id = uploads.get("uploads").toString();
        //запрос к списку видео
        YouTube.PlaylistItems.List playlistItemRequest =
                youtube.playlistItems().list("id,contentDetails");
        playlistItemRequest.setPlaylistId(uploads_id);
        playlistItemRequest.setMaxResults((long)2);
        playlistItemRequest.setFields(
                "items(contentDetails/videoId),nextPageToken,pageInfo");
        String nextToken = "";
        int count=0;
        //do {
            playlistItemRequest.setPageToken(nextToken);
            //получаем список из 50 видео
            PlaylistItemListResponse playlistItemResult = playlistItemRequest.execute();
            //берем их id для получения информации в дальнейшем
            Iterator<PlaylistItem> iteratorSearchResults = playlistItemResult.getItems().iterator();
            while (iteratorSearchResults.hasNext()) {
                PlaylistItem singleVideo = iteratorSearchResults.next();
                    String video_id = singleVideo.getContentDetails().getVideoId();
                    idList.add(video_id);
            }
            nextToken = playlistItemResult.getNextPageToken();
            count+=playlistItemResult.getItems().size();
//        } while (nextToken != null);

        System.out.println("всего видео у данного канала:"+count);
        return idList;
    }
    private static void get_video_id(Iterator<SearchResult> iteratorSearchResults) {
        if (!iteratorSearchResults.hasNext()) {
            //   System.out.println(" There aren't any results for your query.");
        }
//перебираем список видео
        while (iteratorSearchResults.hasNext()) {
            SearchResult singleVideo = iteratorSearchResults.next();
            ResourceId rId = singleVideo.getId(); //id видео
            if (rId.getKind().equals("youtube#video")) {
                String id = rId.getVideoId();
                ids.add(id); //здесь берем только id для дальнейшего получения данных
            }
        }
    }

}
