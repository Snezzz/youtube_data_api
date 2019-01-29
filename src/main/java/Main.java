import com.google.api.client.util.DateTime;
import com.google.api.services.samples.youtube.cmdline.data.ChannelVideo;
import com.google.api.services.samples.youtube.cmdline.data.Search;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.google.gson.Gson;
import org.apache.commons.collections4.list.TreeList;
import org.gephi.preview.api.G2DTarget;
import org.gephi.project.api.ProjectController;
import org.joda.time.Instant;
import org.openide.util.Lookup;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.math.BigInteger;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



//import com.google.api.services.samples.youtube.cmdline.data.Search;

public class Main {

    //ids - коллекция id всех видео по запросу, data - коллекция для сортировки по дате добавления,
    //new_map - отсортированная коллекция
    private static List<String> ids;
    private static Map<String, Map<String, String>> data;
    private static Map<String, Map<String, String>> new_map;
    private static Map<String, Map<String, Map<String, String>>> comments; //коллекция данных о комментариях к видео
    private static Map<String,List<String>> nodes;
    private static String comments_count;
    public static Map<String, Map<String, Map<String, String>>> nodes_map; //коллекция данных о комментариях к видео
    private static  Map <String, Map<String,String>> main_nodes_map;
    public static String current_author_id;
    public static Map<String,Integer> comment_count;
   // private static G2DTarget target1,target2;
    private static int col=0;
    public static G2DTarget target;

    private static XMLStreamWriter out;
    private static  List tags_away;
    public static ProjectController pc;

    public static void main(String[] args) throws IOException, ParserConfigurationException, XMLStreamException, SQLException {

        nodes=new LinkedHashMap<String, List<String>>();
        comment_count=new HashMap<String, Integer>();
        OutputStream outputStream = new FileOutputStream(new File("doc.xml"));

        out = XMLOutputFactory.newInstance().createXMLStreamWriter(
                new OutputStreamWriter(outputStream, "utf-8"));

        out.writeStartDocument();
        out.writeStartElement("Root");

        //create_XLS();
        ChannelVideo channel=new ChannelVideo();
        Search search; //делаем запрос
        search = new Search();
        double time_before= System.currentTimeMillis();
        List<SearchResult> result = search.searchResultList; //получаем результат
        comments = new HashMap<String, Map<String, Map<String, String>>>();
        ids = new ArrayList<String>();
        data = new HashMap<String, Map<String, String>>();
        nodes_map=new HashMap<String, Map<String, Map<String, String>>>();
        //если список не пуст, выводим
        if (result != null) {
            //1.собираем все id видео из полученного списка для дальнейшего получения данных
            get_video_id(result.iterator(), Search.queryTerm);
            YouTube.Videos.List videosListByIdRequest = Search.youtube.videos().list("id,statistics," +
                    "snippet,recordingDetails");
            //2.получаем всю необходимую информацию
            get_info(videosListByIdRequest, comments, Search.apiKey);
        }
        out.writeEndDocument();
        out.close();
        double time_after=System.currentTimeMillis();
        double result_time=(time_after-time_before)/1000;
        System.out.println("Время выполнения запросов:"+result_time+" c.");
        //создаем результативный .xls
       // create_XLS();
    //3.визуализация
        pc = Lookup.getDefault().lookup(ProjectController.class);
        JFrame menu=new JFrame("Main");
        menu.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final JPanel panel=new JPanel();
        final JTextField count=new JTextField(2);
        count.setText("1");
        String[] items = {
                "Betweeness_centrality",
                "Page_rank",
                "Modularity"
        };
        final JComboBox box=new JComboBox(items);
        box.setEditable(false);
        final JButton button=new JButton("Построить");

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean betweeness = false;
                boolean page_rank=false;
                boolean modularity=false;
                int k=Integer.valueOf(count.getText());
                String type=box.getSelectedItem().toString();
                if(type.equals("Betweeness_centrality"))
                    betweeness =true;
                else if(type.equals("Page_rank"))
                    page_rank=true;
                else  if(type.equals("Modularity"))
                    modularity=true;
                try {
                    G2DTarget target = null;
                    new createGraph(k,"",betweeness,page_rank,modularity,target);//создание графа
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }
        });

        panel.add(count);
        panel.add(box);
        panel.add(button);
        menu.add(panel);

        menu.setSize(200,200);
        menu.setLocation(400,150);

        menu.setVisible(true);

        System.out.println("Время выполнения программы с построением графа:"+(System.currentTimeMillis()-time_before)/1000);

    }

    //метод сбора id видео из всего списка = 1 запрос
    private static void get_video_id(Iterator<SearchResult> iteratorSearchResults, String query) {
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

    //2 запрос = сбор всей необходимой информации
    private static void get_info(YouTube.Videos.List videosListByIdRequest, Map<String,
            Map<String, Map<String, String>>> comments,
                                 String apiKey) throws IOException, XMLStreamException, SQLException {
        Iterator<String> iter = ids.iterator();

        //обращаемся к каждому видео для получения данных
        while (iter.hasNext()) {
            out.writeStartElement("Item");
             out.writeStartElement("ID");

           // out.writeStartElement("ID");
            Map<String, Map<String, String>> comments_data = new TreeMap<String, Map<String, String>>();
            //берем данные о каждом видео
            String id = iter.next();
            //ID
            out.writeCharacters(id);
            out.writeEndElement();

            //в запросе задаем id видео
            videosListByIdRequest.setId(id);
            //API KEY
            videosListByIdRequest.setKey(apiKey);
            //запрашиваем
            VideoListResponse listResponse = videosListByIdRequest.execute();

            //собираем все возможные данные по видео
            Map<String, String> map = getInformation(listResponse);
            test(map.get("title"));

            //второй запрос - данные об авторе(канале) данного видео
            YouTube.Channels.List channelsList= Search.youtube.channels().list("statistics,snippet,status,brandingSettings,localizations");
            channelsList.setId(map.get("channel_id"));
            channelsList.setKey(apiKey);
            ChannelListResponse listResponse2 = channelsList.execute();
            getChannelInfo(listResponse2,map);

            out.writeStartElement("Header");
            out.writeCharacters(map.get("title"));
            out.writeEndElement();
            out.writeStartElement("Body");
            out.writeCharacters(map.get("description"));
            out.writeEndElement();
            out.writeStartElement("Username");
            out.writeCharacters(map.get("author"));
            out.writeEndElement();
            out.writeStartElement("UserID");
            out.writeCharacters(map.get("channel_id"));
            out.writeEndElement();
            //является ли канал проверенным Confirmed
            out.writeStartElement("Confirmed");
            out.writeCharacters(map.get("confirmed"));
            out.writeEndElement();
            //unixtime
            Instant instant = Instant.parse( map.get("date") );
            long millisecondsSinceUnixEpoch = instant.getMillis() ;
            out.writeStartElement("Date");
            out.writeCharacters(String.valueOf(millisecondsSinceUnixEpoch));
            out.writeEndElement();
            out.writeStartElement("Request");
            out.writeCharacters(Search.queryTerm);
            out.writeEndElement();
            //теги
            out.writeStartElement("Tags");

            Iterator <String> it=tags_away.iterator();
            while (it.hasNext()){
                out.writeStartElement("Tag");
                out.writeCharacters(it.next());
                out.writeEndElement();
            }
            out.writeEndElement();

            out.writeStartElement("Details");
             out.writeStartElement("Emotions");
                 out.writeEmptyElement("Emotion");
                 out.writeAttribute("type","like");
                 out.writeAttribute("count",map.get("likes_count"));
            out.writeEmptyElement("Emotion");
            out.writeAttribute("type","dislike");
            out.writeAttribute("count",map.get("dislikes_count"));
            //out.writeEndElement();
            out.writeEndElement();
            out.writeEmptyElement("Views");
            out.writeAttribute("count",map.get("view_count"));

            //автор данного видео
            current_author_id=map.get("channel_id");
            main_nodes_map=new HashMap<String, Map<String, String>>();

            comments_count=map.get("comments_count");
            if(!map.get("comments_count").equals("0")) {

               CommentThreadListResponse videoCommentsListResponse = Search.youtube.commentThreads()
                       .list("id,snippet").setVideoId(id).setTextFormat("plainText")
                       .setMaxResults((long) 100)
                       .execute();
              // out.writeStartElement("Comments");
               // out.writeEndElement();
              getVideoComments(videoCommentsListResponse, id, comments_data, comments, nodes_map, main_nodes_map);
               String nextPage = videoCommentsListResponse.getNextPageToken();

               //перебираем все страницы = следующие запросы
               do {
                   videoCommentsListResponse = Search.youtube.commentThreads()
                           .list("id,snippet").setVideoId(id).setTextFormat("plainText")
                           .setPageToken(nextPage)
                           .setMaxResults((long) 100).execute();
                   getVideoComments(videoCommentsListResponse, id, comments_data, comments, nodes_map, main_nodes_map);
                   nextPage = videoCommentsListResponse.getNextPageToken();
               }
               while (nextPage != null);
               //заполняем карту данных для удобства
               //ключ - тот, на кого ссылаются, значение - кто ссылается
               data.put(id, map);

               comments.put(id, comments_data); //ДОБАВЛЯЕМ ВСЕ КОММЕНТАРИИ К ВИДЕО

           }
            else{
                out.writeEmptyElement("Comments");
                out.writeAttribute("count",comments_count);
            }
            //details
            out.writeEndElement();
            //item
            out.writeEndElement();
        }



        //root
        out.writeEndElement();
        System.out.println(comments.size());
        //сортировка видео по дате выкладывания
        sort(data);

        //запись результата в документ(требовалось ранее)
        //   write(new_map);

        //добавление данных о видео в БД
       // load_data("postgres", "qwerty", new_map, false, null);
        //загружаем комментарии на базу данных: для каждого видео - отдельный запрос
        FileWriter writer = new FileWriter("example.json");
        writer.write("{");
        Map <String,Integer> video_comments;
        for (Map.Entry<String, Map<String, Map<String, String>>> entry : comments.entrySet()) {
           // load_data("postgres", "qwerty", entry.getValue(), true, entry.getKey());
            //video_comments=new HashMap<String, Integer>();
            //get_comments_count("postgres", "qwerty",entry.getKey(),video_comments);
            create_json(writer,entry.getValue());
        }
        writer.write("}");
        writer.close();
    }

    private static void get_comments_count(String user,String password,String videoID,
                                              Map<String,Integer> map) throws SQLException {
        Connection c;
        Statement stmt;
        ResultSet resultSet=null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5431/postgres", user, password);
            c.setAutoCommit(false);
            System.out.println("-- Opened database successfully");
            String sql;
            stmt = c.createStatement(); //открываем соединение
            sql="select comment_author,author_id,parent_id,count(parent_id)" +
                    " from comments where video_id='"+videoID+"'" +
                    " group by comment_author,author_id,parent_id";
            resultSet=stmt.executeQuery(sql);
            while (resultSet.next()){
                String who_to_whom=resultSet.getString("author_id")+"!"
                        +resultSet.getString("parent_id");
               comment_count.put(who_to_whom,
                        Integer.valueOf(resultSet.getString("count")));
            }


        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
        //сбор информации о видео в коллекцию
    private static Map<String, String> getInformation(VideoListResponse listResponse) {

        Video current_video = listResponse.getItems().get(0);
        String title = current_video.getSnippet().getTitle(); //название видео
        String author = current_video.getSnippet().get("channelTitle").toString(); //название канала
        DateTime time = current_video.getSnippet().getPublishedAt();//дата публикации
        String date = time.toString().substring(0, 19);
        String tags = "";
        tags_away=new TreeList();
        //теги, если есть
        if (current_video.getSnippet().getTags() != null) {
            tags = current_video.getSnippet().getTags().toString();
            tags_away=current_video.getSnippet().getTags();
        }
        //System.out.println(current_video.getContentDetails());
        String description = current_video.getSnippet().getDescription(); //описание видео
        BigInteger view_count = current_video.getStatistics().getViewCount();//число просмотров
        //количество лайков
        BigInteger likes_count = null;
        if (current_video.getStatistics().getLikeCount() != null) {
            likes_count = current_video.getStatistics().getLikeCount();
        }
        //количество дизлайков
        BigInteger dislikes_count = null;
        if (current_video.getStatistics().getDislikeCount() != null) {
            dislikes_count = current_video.getStatistics().getDislikeCount();
        }
        //количество комментариев
        BigInteger comments_count = null;
        if (current_video.getStatistics().getCommentCount() != null) {
            comments_count = current_video.getStatistics().getCommentCount();
        }
//заносим данные в карту
        Map<String, String> data = new HashMap<String, String>();
        data.put("title", title);
        data.put("author", author);
        data.put("date", date);
        data.put("description", description);
        data.put("channel_id", current_video.getSnippet().getChannelId());
        if (view_count == null)
            data.put("view_count", "0");
        else
            data.put("view_count", view_count.toString());
        if (likes_count == null)
            data.put("likes_count", "0");
        else
            data.put("likes_count", likes_count.toString());
        if (dislikes_count == null)
            data.put("dislikes_count", "0");
        else
            data.put("dislikes_count", dislikes_count.toString());
        if (comments_count == null)
            data.put("comments_count", "0");
        else
            data.put("comments_count", comments_count.toString());

        data.put("tags", tags);
        return data;
    }

    //сбор информации о канале
    private static void getChannelInfo(ChannelListResponse listResponse, Map<String, String> data) {

        Channel current_channel = listResponse.getItems().get(0);
        String status=current_channel.getStatus().getLongUploadsStatus();
        boolean result_status=false;
        if(status.equals("allowed")||(status.equals("eligible"))){
            result_status=true;
        }
        String country=current_channel.getBrandingSettings().getChannel().getCountry();
        data.put("country",country);
        data.put("confirmed",String.valueOf(result_status));
        /*BigInteger follower_count = current_channel.getStatistics().getSubscriberCount();
        data.put("follower_count", follower_count.toString());
        BigInteger video_count = current_channel.getStatistics().getVideoCount();
        data.put("video_count", video_count.toString());
        String description = "";
        try {
            description = current_channel.getSnippet().getDescription();
        } catch (Exception e) {
            System.out.println(e);
        }

        data.put("channel_info", description);
*/
    }

    //сбор комментариев
    private static void getVideoComments(CommentThreadListResponse videoCommentsListResponse,
                                         String id, Map<String, Map<String, String>> map,
                                         Map<String,Map<String, Map<String, String>>> comments,
                                         Map<String,Map<String, Map<String, String>>> nodes,
                                         Map <String, Map<String,String>> nodes_map) throws IOException, XMLStreamException {
        List<CommentThread> videoComments = videoCommentsListResponse.getItems();
        //каждый комментарий
        if (videoComments.size() > 0) {
            out.writeStartElement("Comments");
            out.writeAttribute("count",comments_count);
            for (CommentThread videoComment : videoComments) {
                Map<String, String> data = new HashMap<String, String>();
                List<String> id_of_repliers;
                CommentSnippet snippet = videoComment.getSnippet().getTopLevelComment()
                        .getSnippet();
                String data_id = snippet.getAuthorChannelId().toString().substring(7);
                String author_id = data_id.substring(0, data_id.length() - 1);
                out.writeStartElement("Comment");
                out.writeStartElement("Username");
                out.writeCharacters(snippet.getAuthorDisplayName());
                out.writeEndElement();
                out.writeStartElement("UserID");
                out.writeCharacters(author_id);
                out.writeEndElement();
                out.writeStartElement("Text");
                out.writeCharacters(snippet.getTextDisplay());
                out.writeEndElement();

                //заполнение карты comments
                data.put("video_id", id);
                data.put("comment_id", videoComment.getId());
                data.put("author", snippet.getAuthorDisplayName());
                data.put("author_id", author_id);
                data.put("parent_id", current_author_id);
                //  System.out.println("comment_author:"+snippet.getAuthorDisplayName());
                data.put("comment", snippet.getTextDisplay());
                data.put("date", snippet.getPublishedAt().toString());
                data.put("language", snippet.getTextOriginal());
                //все главные комментарии
                //комментарий - данные
                map.put(videoComment.getId(), data);

                //заполняем карту вершин

                // if(nodes.containsKey(current_author_id))
                data = new HashMap<String, String>();
                data.put("author_id", author_id);
                data.put("author_name", snippet.getAuthorDisplayName());
                data.put("parent_id", current_author_id);
                data.put("video_id", id);
                nodes_map.put(author_id, data); //author_id - адрес автора комментария

                //ответы(дополняется map)
                CommentListResponse commentsListResponse = Search.youtube.comments().list("snippet")
                        .setParentId(videoComment.getId()).execute();

                get_replies(snippet, id, author_id, videoComment.getId(), nodes, map);

                out.writeEndElement();
            }

            nodes.put(current_author_id, nodes_map);

            //comments
            out.writeEndElement();
        }

    }
//ответы на комментарии
    private static void get_replies(CommentSnippet snippet,
                                    String video_id,
                                    String comment_author_id,
                                    String id,
                                    Map<String,Map<String,Map<String,String>>> nodes,
                                         Map<String, Map<String, String>> map)
            throws IOException, XMLStreamException {

        //долго

        CommentListResponse commentsListResponse = Search.youtube.comments().list("snippet")
                .setParentId(id).execute();
        List<Comment> comments = commentsListResponse.getItems();

        //Map <String,Map<String,String>> data=new TreeMap<String, Map<String, String>>();
       Map <String,String> information;
       // Map <String,String> connection=new TreeMap<String,String>();
        Map<String,Map <String,String>> help_data=new HashMap<String, Map<String, String>>();
       // List <String> ids= new TreeList<String>();

        String parent_id=id;
        //String parent_id=comment_author_id;
        if (comments.isEmpty()) {
            //System.out.println("There aren't comment replies.");
        } else {
//все ответы
            //out.writeStartElement("Answers");
            long time=System.currentTimeMillis();
            out.writeStartElement("Comments");
            out.writeAttribute("count",String.valueOf(comments.size()));
            for (Comment commentReply : comments) {

                information = new HashMap<String, String>();
                snippet = commentReply.getSnippet();
               String data_id=snippet.getAuthorChannelId().toString().substring(7);
                String author_id=data_id.substring(0,data_id.length()-1);
                out.writeStartElement("Comment");
                out.writeStartElement("Username");
                out.writeCharacters(snippet.getAuthorDisplayName());
                out.writeEndElement();
                out.writeStartElement("UserID");
                out.writeCharacters(author_id);
                out.writeEndElement();
                out.writeStartElement("Text");
                out.writeCharacters(snippet.getTextDisplay());
                out.writeEndElement();
                out.writeEndElement();

                //заполнение карты comments
                information.put("video_id",video_id);
                information.put("comment_id", commentReply.getId());
                information.put("author", snippet.getAuthorDisplayName());
                information.put("author_id",author_id);
                information.put("parent_id",comment_author_id);
                information.put("comment", snippet.getTextDisplay());
                information.put("date", snippet.getPublishedAt().toString());
                information.put("language", snippet.getTextOriginal());  //добавляем к главным комментариям
                map.put(commentReply.getId(),information); //в сам комментарий
                //очистка
                information=new HashMap<String, String>();

               //данные о вершине

                information.put("author_id",author_id);
                information.put("author_name",snippet.getAuthorDisplayName());
                information.put("parent_id",snippet.getParentId());
                information.put("video_id",video_id);
                //id автора - информация
                help_data.put(author_id,information);
               //дополняем главную коллекцию

            }
            out.writeEndElement();
            //???????
            //если уже был упомянут данный пользователь(повторный ответ)
            if(nodes.containsKey(parent_id)){
                //старые данные
                Map<String,Map<String,String>> old_data=nodes.get(parent_id);
                //беребираем все старые данные и проверяем, нет ли еще данного пользователя в
                //ссылающихся
              for(Map.Entry<String, Map<String,String>> entry : old_data.entrySet()){
                  if(help_data.containsKey(entry.getKey())){
                     continue;
                  }
                  //если нет, добавляем данные
                  help_data.put(entry.getKey(),entry.getValue());
              }
            }

            //заносим данные в  коллекцию вершин
               nodes.put(comment_author_id,help_data);

        }
       // System.out.println(System.currentTimeMillis());
    }

    //сортировка видео по дате выкладывания (быстрая сортировка)
    private static void sort(Map<String, Map<String, String>> data) {

        Date date = null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Map<Date, String> data_map = new HashMap<Date, String>();
        new_map = new LinkedHashMap<String, Map<String, String>>();
        Date[] data_list = new Date[data.size()];
        List<String> sort_ids = new LinkedList<String>();
        int i = 0;
        for (Map.Entry<String, Map<String, String>> entry : data.entrySet()) {
            try {
                date = formatter.parse(entry.getValue().get("date")); //в дату
                data_map.put(date, entry.getKey());
                data_list[i] = date;
                i++;
            } catch (Exception e) {
                System.out.println(e);
            }
        }

//дата до
        for (int j = 0; j < data_list.length; j++) {
            System.out.println(data_list[j]);
        }

        //сортировка дат
        quickSort(data_list, 0, data_list.length - 1);
        for (int j = 0; j < data_list.length; j++) {
            //    System.out.println(data_list[j]);
        }
        for (int j = 0; j < data_list.length; j++) {
            System.out.println(data_list[j]);
            Map<String, String> current_data = data.get(data_map.get(data_list[j]));
            new_map.put(data_map.get(data_list[j]), current_data);
        }

        /*
        //дата после
        for (Map.Entry<String, Map<String, String>> entry : new_map.entrySet()) {
            System.out.println(entry.getKey() + entry.getValue().get("date"));
        }
        */
    }
//быстрая сортировка
    private static void quickSort(Date[] array, int low, int high) {
        if (array.length == 0)
            return;//завершить выполнение если длина массива равна 0

        if (low >= high)
            return;//завершить выполнение если уже нечего делить

        //1.выбрать опорный элемент
        int middle = low + (high - low) / 2;
        Date opora = array[middle];

        //2.разделить на подмассивы, который больше и меньше опорного элемента
        int i = low, j = high;
        while (i <= j) {
            //текущее меньше опорного
            while (array[i].compareTo(opora) == -1) {
                i++;
            }
            //текущее больше опорного
            while (array[j].compareTo(opora) == 1) {
                j--;
            }
//меняем местами элементы
            if (i <= j) {
                Date temp = array[i];
                array[i] = array[j];
                array[j] = temp;
                i++;
                j--;
            }
        }

        // рекурсия для сортировки левой и правой части
        if (low < j)
            quickSort(array, low, j);

        if (high > i)
            quickSort(array, i, high);
    }

    //запись в документ
    private static void write(Map<String, Map<String, String>> from) throws IOException {
        FileWriter writer = new FileWriter("youtube_data.txt");
        for (Map.Entry<String, Map<String, String>> entry : from.entrySet()) {
            writer.write("Видео:" + entry.getKey());
            writer.append('\n');
            for (Map.Entry<String, String> entry2 : entry.getValue().entrySet()) {
                writer.append(entry2.getKey() + ":");
                writer.append(entry2.getValue());
                writer.append('\n');
            }
            writer.append('\n');
            writer.append("=============================================================\n");
        }
        writer.flush();

    }

    //занесение данных в БД POSTGRESQL
    private static void load_data(String user, String password, Map<String, Map<String, String>> from,
                                  boolean comments, String video_id) {
        Connection c;
        Statement stmt;
        double i = Math.random();
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5431/postgres", user, password);
            c.setAutoCommit(false);
            System.out.println("-- Opened database successfully");
            String sql;
            stmt = c.createStatement(); //открываем соединение
            //заполнение таблицы comments
            if (comments) {
                for (Map.Entry<String, Map<String, String>> entry : from.entrySet()) {
                    sql = "INSERT INTO postgres.public.comments (comment_id, video_id, comment_text, comment_author, " +
                            "comment_date,author_id,parent_id,real_text) VALUES (?,?,?,?,?,?,?,?)";
                    PreparedStatement stat = c.prepareStatement(sql);
                    stat.setString(1, entry.getValue().get("comment_id"));
                    stat.setString(2, video_id);
                    stat.setString(3, entry.getValue().get("comment"));
                    stat.setString(4, entry.getValue().get("author"));
                    stat.setString(5, entry.getValue().get("date"));
                    stat.setString(6, entry.getValue().get("author_id"));
                    stat.setString(7, entry.getValue().get("parent_id"));
                    stat.setString(8, entry.getValue().get("language"));

                    stat.executeUpdate();
                }
            }
            //заполнение таблицы данных о видео
            else {
                for (Map.Entry<String, Map<String, String>> entry : from.entrySet()) {
                    sql = "INSERT INTO postgres.public.videos (video_id,id,video_title,author,publication_date,description,view_count" +
                            ",likes_count,dislikes_count,comments_count,tags,channel_id,channel_follovers_count," +
                            "channel_video_count,channel_description) VALUES ('" + entry.getKey() + "'," + i + ",'"
                            + entry.getValue().get("title") + "','" + entry.getValue().get("author") + "','"
                            + entry.getValue().get("date") + "','" + entry.getValue().get("description") + "',"
                            + entry.getValue().get("view_count") + "," + entry.getValue().get("likes_count") + ","
                            + entry.getValue().get("dislikes_count") + "," + entry.getValue().get("comments_count") + ",'"
                            + entry.getValue().get("tags") + "' ,'" + entry.getValue().get("channel_id") + "'," +
                            entry.getValue().get("follower_count") + "," + entry.getValue().get("video_count") + ",'" +
                            entry.getValue().get("channel_info") + "')";
                    i++;
                    stmt.executeUpdate(sql); //заносим данные
                }
            }
            stmt.close();
            c.commit();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
//запись информации о комментаторах в json
    private static void create_json(FileWriter writer, Map<String, Map<String, String>> map) {
    Connection c;
    Statement stmt;
    try {
        Class.forName("org.postgresql.Driver");
        c = DriverManager
                .getConnection("jdbc:postgresql://localhost:5431/postgres", "postgres", "qwerty");
        c.setAutoCommit(false);
        System.out.println("-- Opened database successfully");
        String sql;
        stmt = c.createStatement(); //открываем соединение
        sql="select * from comments";
        Gson gson = new Gson();

        ResultSet rs = stmt.executeQuery(sql);
        Map<String,String> data_map =new TreeMap<String, String>();
        List<String> result=new LinkedList<String>();
        //JSONWriter json=new JSONStringer();
        while (rs.next()){

            data_map.put("video_id",rs.getString("video_id"));
            String id=rs.getString("author_id");
            String  convert=id.substring(7);
            String author_id=convert.substring(0,convert.length()-1);
            data_map.put("author_id",rs.getString("author_id"));
            data_map.put("comment_author",rs.getString("comment_author"));
            data_map.put("comment_text",rs.getString("comment_text"));
            data_map.put("comment_date",rs.getString("comment_date"));
            result.add('"'+author_id+'"'+":"+gson.toJson(data_map));
            result.add(",");
        }
        for(int i=0;i<result.size();i++) {
            writer.append(result.get(i));
        }


    } catch (ClassNotFoundException e) {
        e.printStackTrace();
    } catch (SQLException e) {
        e.printStackTrace();
    } catch (IOException e) {
        e.printStackTrace();
    }

}
/*
    private static void create_XLS() throws IOException {
        Connection c;
        Statement stmt;
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("Edges");

        //sheet.autoSizeColumn(1);
        Cell cell;
        HSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);

        Row row;
        int rownum = 0;

        int i = 0;//id
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5431/postgres", "postgres", "qwerty");
            c.setAutoCommit(false);
            System.out.println("-- Opened database successfully");
            String sql;
            stmt = c.createStatement(); //открываем соединение

            //создание 1й таблицы Edge
            sql = "SELECT channel_id as channel,comment_author,author_id FROM videos JOIN comments on" +
                    " videos.video_id = comments.video_id order by channel_id";
            ResultSet rs = stmt.executeQuery(sql);
            int columns = rs.getMetaData().getColumnCount();
            //создание строки
            row = sheet.createRow(rownum);
            cell = row.createCell(1, CellType.STRING);
            cell.setCellValue("target");
            cell.setCellStyle(style);
            cell = row.createCell(2, CellType.STRING);
            cell.setCellValue("comment_author");
            cell.setCellStyle(style);
            cell = row.createCell(3, CellType.STRING);
            cell.setCellValue("who");
            cell.setCellStyle(style);
            rownum++;
            List channels=new LinkedList();
            nodes=new HashMap<String, List<String>>();
            /*
            перебираем все ряды
            1.Если мы еще не рассматривали данную вершину, создаем для нее новый список ссылающихся
            Если рассматривали, заполняем предыдущий set смежных вершин
             2.обновляем карту

            String prev="";
            while (rs.next()) {

                row = sheet.createRow(rownum);
                String channel = rs.getString("channel");

                String comment_author = rs.getString("comment_author");
                String author_id = rs.getString("author_id");

                cell = row.createCell(1, CellType.STRING);
                cell.setCellValue(channel);
                cell = row.createCell(2, CellType.STRING);
                cell.setCellValue(comment_author);
                cell = row.createCell(3, CellType.STRING);
                //author_id = author_id.substring(7);
                //String[] res = author_id.split("}");
                //author_id = res[0];
                cell.setCellValue(author_id);
                rownum++;
                //перешли на другое видео
                if((!channel.equals(prev))&&(!prev.equals(""))){
                    nodes.put(prev,channels);
                    channels=new LinkedList();
                }
                channels.add(author_id);
                prev=channel;
            }
            //последнее видео
            nodes.put(prev,channels);

            sheet = workbook.createSheet("Nodes");
            rownum = 1;
            row = sheet.createRow(rownum);
            cell = row.createCell(1, CellType.STRING);
            cell.setCellValue("node");
            cell = row.createCell(2, CellType.STRING);
            cell.setCellValue("author");
            rownum++;
            //создание второй таблицы Nodes
            sql = "select DISTINCT channel_id,author from videos";
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                row = sheet.createRow(rownum);
                String channel = rs.getString("channel_id");
                String author = rs.getString("author");
                cell = row.createCell(1, CellType.STRING);
                cell.setCellValue(channel);
                cell = row.createCell(2, CellType.STRING);
                cell.setCellValue(author);
                rownum++;
            }
            //дополнение таблицы вершин(там только авторы видео) авторами комментариев
            sql = "select author_id,comment_author from comments";
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                row = sheet.createRow(rownum);
                String author_id = rs.getString("author_id");
                String comment_author = rs.getString("comment_author");
                cell = row.createCell(1, CellType.STRING);
                author_id = author_id.substring(7);
                String[] res = author_id.split("}");
                author_id = res[0];
                cell.setCellValue(author_id);
                cell = row.createCell(2, CellType.STRING);
                cell.setCellValue(comment_author);

                rownum++;
            }
            sql = "with tt as (\n" +
                    "    select\n" +
                    "      comment_author,\n" +
                    "      comment_id,video_id\n" +
                    "    from comments\n" +
                    "),t as(\n" +
                    "    select\n" +
                    "      tt.comment_author,\n" +
                    "      count(tt.comment_id) as comments_count,\n" +
                    "      count(distinct tt.video_id) as video_count\n" +
                    "    from tt\n" +
                    "  group by tt.comment_author\n" +
                    ")\n" +
                    "select * from t\n" +
                    "order by comments_count desc";
            rs = stmt.executeQuery(sql);
            sheet = workbook.createSheet("Количество комментариев");
            rownum = 1;
            row = sheet.createRow(rownum);
            cell = row.createCell(1, CellType.STRING);
            cell.setCellValue("User");
            cell = row.createCell(2, CellType.STRING);
            cell.setCellValue("comments_count");
            cell = row.createCell(3, CellType.STRING);
            cell.setCellValue("video_count");
            rownum++;

            while (rs.next()) {
                row = sheet.createRow(rownum);
                String comment_author = rs.getString("comment_author");
                String comments_count = rs.getString("comments_count");
                String video_count = rs.getString("video_count");
                cell = row.createCell(1, CellType.STRING);
                cell.setCellValue(comment_author);
                cell = row.createCell(2, CellType.STRING);
                cell.setCellValue(comments_count);
                cell = row.createCell(3, CellType.STRING);
                cell.setCellValue(video_count);

                rownum++;
            }
            stmt.close();
            c.commit();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //
        File file = new File("data.xls");
        //file.getParentFile().mkdirs();

        FileOutputStream outFile = new FileOutputStream(file);
        workbook.write(outFile);

    }
*/
//выявление тегов из названия видео
    private static void test(String testString) {
        Pattern p = Pattern.compile("\\S+");
        Matcher m = p.matcher(testString); //выполняем проверка
        while (m.find()) {
            String tag = testString.substring(m.start(), m.end());
            tags_away.add(tag);
        }
    }

}