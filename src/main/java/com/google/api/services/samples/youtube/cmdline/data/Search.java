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
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Properties;
/**
 * Prints a list of videos based on a search term.
 *
 * @author Jeremy Walker
 */
public class Search {

    private static String PROPERTIES_FILENAME = "youtube.properties";
    private static long NUMBER_OF_VIDEOS_RETURNED;
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

    public Search() throws IOException {

        // create_XLS();
        //Считывает api ключ пользователя
        Properties properties = new Properties();

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
            //1.Получаем данные для запроса
            queryTerm = getInputQuery();
            System.out.println("query="+queryTerm);
            //настраиваем GET-запрос на ютуб
            YouTube.Search.List search = youtube.search().list("id,snippet");

            //заносим настройки в запрос - API ключ
            apiKey = properties.getProperty("youtube.apikey");

            search.setKey(apiKey);
            search.setQ(queryTerm); //передаем сам запрос

            //указываем, что ищем
            search.setType("video");
            //выбираем из результата запроса только те данные, которые нас интересуют
            search.setFields("items(id/kind,id/videoId,snippet/title,snippet/description,snippet/channelTitle," +
                    "snippet/thumbnails/default/url)");
            search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
            // System.out.println(search);
            //отправляем запрос на сервер
            SearchListResponse searchResponse = search.execute();

            //получаем результат запроса в виде списка
            searchResultList = searchResponse.getItems();

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
    private static String getInputQuery() throws IOException {

        String inputQuery = "";
        String encoding = System.getProperty("console.encoding", "utf-8");
        BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in,encoding));

        System.out.print("Введите максимальное количество обрабатываемого видео:");

        NUMBER_OF_VIDEOS_RETURNED = Integer.valueOf(bReader.readLine());

        System.out.print("Введите запрос: ");
        inputQuery = bReader.readLine();

        if (inputQuery.length() < 1) {
            // If nothing is entered, defaults to "YouTube Developers Live."
            inputQuery = "YouTube Developers Live";
        }
        return inputQuery;
    }
}
