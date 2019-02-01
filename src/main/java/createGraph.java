import com.google.api.services.samples.youtube.cmdline.data.ChannelVideo;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.gephi.appearance.api.*;
import org.gephi.appearance.plugin.PartitionElementColorTransformer;
import org.gephi.appearance.plugin.RankingElementColorTransformer;
import org.gephi.appearance.plugin.RankingNodeSizeTransformer;
import org.gephi.appearance.plugin.palette.Palette;
import org.gephi.appearance.plugin.palette.PaletteManager;
import org.gephi.graph.api.*;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.openord.OpenOrdLayout;
import org.gephi.preview.api.*;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.Workspace;
import org.gephi.statistics.plugin.*;
import org.openide.util.Lookup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class createGraph extends JFrame {


    public static DirectedGraph directedGraph;
    private Map<String,Node> points;
    private static Map<String,Double> betweeness_centrality;
    private static Map<String,Double> closeness_centrality;
    private static Map<String,Double> eigenvector_centrality;
    private static Map<String,Double> ECCENTRICITY ;
    private static Map<String,Double> PAGERANK ;
    private static Map<String,Double> authority ;
    private static Map<String,Double> hub;
    private static Map<String,Double> DEGREE;
    private static Map<String,Double> WEIGHTEDDEGREE;
    private static final String COMMA_DELIMITER = ",";
    private static final String NEW_LINE_SEPARATOR = "\n";
    private static final String FILE_HEADER = "node_id,value";
    public static  Workspace workspace;



    public createGraph(int n, String title, boolean Default,boolean betweeness, boolean pagerank, boolean module,
                       G2DTarget target) throws IOException {
        super(title);

        //создание нового проекта
        Main.pc.newProject();
        workspace = Main.pc.getCurrentWorkspace();

        //модель графа
        GraphModel graphModel = Lookup.getDefault()
                .lookup(GraphController.class).getGraphModel();
        directedGraph = graphModel.getDirectedGraph();
        //создание узлов и ребер на directedGraph
        create(n,graphModel,directedGraph,points);

        //анализ
        get_analysis(graphModel);
       // write_results();
        //укладка графа по заданному алгоритму
        stowage("YifanHu",graphModel);

        //BETWEENESS_CENTRALITY
        if(betweeness) {
         //   filter(graphModel, GraphDistance.BETWEENNESS, workspace);
        }
       else if(pagerank) {
            //PageRank
            filter(graphModel, PageRank.PAGERANK, workspace);
        }
        //модулярность
       else if(module) {
            modularity(graphModel, workspace);
        }
        //отображение графа на панели
        build(target);

    }
//анализ графа
    public static void get_analysis(GraphModel graphModel){
        betweeness_centrality=new TreeMap<String, Double>();
        closeness_centrality=new TreeMap<String, Double>();
        eigenvector_centrality=new TreeMap<String, Double>();
        ECCENTRICITY=new TreeMap<String, Double>();
        PAGERANK=new TreeMap<String, Double>();
        authority=new TreeMap<String, Double>();
        hub=new TreeMap<String, Double>();
        DEGREE=new TreeMap<String, Double>();
        WEIGHTEDDEGREE=new TreeMap<String, Double>();
        System.out.println("узлы со степенью > 1 :");
        for(Node n : directedGraph.getNodes()) {
            Node[] neighbors = directedGraph.getNeighbors(n).toArray();
            if(neighbors.length>1) {
                System.out.println(n.getLabel() + " has " + neighbors.length + " neighbors");
            }
        }
        //кратчайшие пути
        GraphDistance distance = new GraphDistance();
        distance.setDirected(true);
        distance.execute(graphModel);

        //System.out.println("Диаметр графа: "+distance.getDiameter());
        //System.out.println("Радиус графа: "+distance.getRadius());

        //System.out.println("Центральность графа: ");
        //System.out.println("1.По посредничеству:");
        for (Node n : directedGraph.getNodes()) {
            Double centrality = (Double)n.getAttribute(GraphDistance.BETWEENNESS);
            betweeness_centrality.put(n.getLabel(),centrality);
        }
       // System.out.println("2.По близости:");
        for (Node n : directedGraph.getNodes()) {
            Double centrality = (Double)n.getAttribute(GraphDistance.CLOSENESS);
            closeness_centrality.put(n.getLabel(),centrality);
        }
        //System.out.println("3.Гармоническая центральность:");
        for (Node n : directedGraph.getNodes()) {
            Double centrality = (Double)n.getAttribute(GraphDistance.HARMONIC_CLOSENESS);

        }
        //центральность по собственному вектору
        EigenvectorCentrality eigenvectorCentrality=new EigenvectorCentrality();
        eigenvectorCentrality.execute(graphModel);
        for (Node n : directedGraph.getNodes()) {
            Double centrality = (Double)n.getAttribute(EigenvectorCentrality.EIGENVECTOR);
            eigenvector_centrality.put(n.getLabel(),centrality);
        }
        //System.out.println("Эксцентричность:");
        for (Node n : directedGraph.getNodes()) {
            Double eccentricity = (Double)n.getAttribute(GraphDistance.ECCENTRICITY);
            ECCENTRICITY.put(n.getLabel(),eccentricity);
        }
        //коэффициент кластеризации
        ClusteringCoefficient coefficient=new ClusteringCoefficient();
        coefficient.execute(graphModel);
        //System.out.println("Коэффициент кластеризации:"+coefficient.getAverageClusteringCoefficient());

        //hits = authority(важность узла) - hubs(ссылки на важные узлы)
        Hits hits=new Hits();
        hits.execute(graphModel);

        //pageRank = приоритет узла
        PageRank pageRank=new PageRank();
        pageRank.execute(graphModel);

        //средняя взвешенная степень
        WeightedDegree weightedDegree=new WeightedDegree();
        weightedDegree.execute(graphModel);

        //степень вершины
        Degree degree=new Degree();
        degree.execute(graphModel);
        for (Node n : directedGraph.getNodes()) {
            //Double authority_value=(Double)n.getAttribute(Hits.AUTHORITY);
            //Double hub_value=(Double)n.getAttribute(Hits.HUB);
            Double rank = (Double)n.getAttribute(PageRank.PAGERANK);
            Double weightDegree=(Double)n.getAttribute(WeightedDegree.WDEGREE);
            Integer degree_value=(Integer)n.getAttribute(Degree.DEGREE);
          //  authority.put(n.getLabel(),authority_value);
            //hub.put(n.getLabel(),hub_value);
            PAGERANK.put(n.getLabel(),rank);
            WEIGHTEDDEGREE.put(n.getLabel(),weightDegree);
            DEGREE.put(n.getLabel(),(double)degree_value);
        }


        //Get modularity for coloring
        Modularity modularity = new Modularity();
        modularity.setUseWeight(true);
        modularity.setRandom(true);
        modularity.setResolution(1.0);
//        modularity.execute(graphModel);
        //выделяем лидеров

    }
    private static void write_results() throws IOException {

        //xls
        File myFile_ED = new File("data.xls");
        FileInputStream inputStream_ED = new FileInputStream(myFile_ED);

        HSSFWorkbook workbook = new HSSFWorkbook(inputStream_ED);

        create_XLSlist(workbook,"betweeness_centrality",betweeness_centrality);
        create_XLSlist(workbook,"closeness_centrality",closeness_centrality);
        create_XLSlist(workbook,"eigenvector_centrality",eigenvector_centrality);
        create_XLSlist(workbook,"ECCENTRICITY",ECCENTRICITY);
        create_XLSlist(workbook,"PAGERANK",PAGERANK);
        create_XLSlist(workbook,"authority",authority);
        create_XLSlist(workbook,"hub",hub);
        create_XLSlist(workbook,"WEIGHTEDDEGREE",WEIGHTEDDEGREE);
        create_XLSlist(workbook,"DEGREE",DEGREE);

//csv
        FileOutputStream os_ED = new FileOutputStream(myFile_ED);
        workbook.write(os_ED);
        os_ED.close();
        create_CSV(betweeness_centrality,"betweeness_centrality");
        create_CSV(closeness_centrality,"closeness_centrality");
        create_CSV(eigenvector_centrality,"eigenvector_centrality");
        create_CSV(ECCENTRICITY,"ECCENTRICITY");
        create_CSV(PAGERANK,"PAGERANK");
        create_CSV(authority,"authority");
        create_CSV(hub,"hub");
        create_CSV(WEIGHTEDDEGREE,"WEIGHTEDDEGREE");
        create_CSV(DEGREE,"DEGREE");

    }
    public static void create_CSV(Map <String,Double> from,String file_name) throws IOException {
        Iterator<Map.Entry<String, Double>> entries = from.entrySet().iterator();
        FileWriter file=null;
        try{
            file=new FileWriter("/results/"+file_name+".csv");
            file.append(FILE_HEADER.toString());
            file.append(NEW_LINE_SEPARATOR);
            while(entries.hasNext()){
                Map.Entry<String, Double> entry = entries.next();
                String id = entry.getKey();
                Double value = entry.getValue();
                file.append(id);
                file.append(COMMA_DELIMITER);
                file.append(value.toString());
                file.append(NEW_LINE_SEPARATOR);
            }
        }
        catch(Exception e){
            System.out.println("Error in CsvFileWriter !!!");
            e.printStackTrace();
        }
        file.close();


    }
    public static void create_XLSlist(HSSFWorkbook workbook,String name,Map <String,Double> from){

        HSSFSheet sheet = workbook.createSheet(name);
        //sheet.autoSizeColumn(1);
        Cell cell;
        HSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);

        Row row;
        int rownum = 0;
        row = sheet.createRow(rownum);
        cell = row.createCell(1, CellType.STRING);
        cell.setCellValue("node");
        cell.setCellStyle(style);
        cell = row.createCell(2, CellType.STRING);
        cell.setCellValue("value");
        cell.setCellStyle(style);

        rownum++;
        Iterator<Map.Entry<String, Double>> entries = from.entrySet().iterator();

        while(entries.hasNext()){
            Map.Entry<String, Double> entry = entries.next();
            row = sheet.createRow(rownum);
            String id = entry.getKey();
            Double value = entry.getValue();
            cell = row.createCell(1, CellType.STRING);
            cell.setCellValue(id);
            cell = row.createCell(2, CellType.STRING);
            cell.setCellValue(value);
            rownum++;
        }
    }

    //укладка графа
    public static void stowage(String type,GraphModel graphModel) {
        if (type.equals("YifanHu")) {
            //YifanHu укладка
            YifanHuLayout layout = new YifanHuLayout(null, new StepDisplacement(1f));
            layout.setGraphModel(graphModel);
            layout.resetPropertiesValues();
            layout.setOptimalDistance(200f);
            layout.initAlgo();
            for (int i = 0; i < 200 && layout.canAlgo(); i++) {
                layout.goAlgo();
            }
        } else if(type.equals("OpenOrd")) {
            OpenOrdLayout layout = new OpenOrdLayout(null);
            layout.setGraphModel(graphModel);
            layout.resetPropertiesValues();
            layout.initAlgo();
            for (int i = 0; i < 200 && layout.canAlgo(); i++) {
                layout.goAlgo();
            }
        }
    }

    //создание графа
    private static void create(int degree, GraphModel graphModel,DirectedGraph directedGraph, Map<String,Node> points) {
        Random rand = new Random();
        Iterator iterator = Main.nodes_map.entrySet().iterator();
        directedGraph = graphModel.getDirectedGraph();
        points = new TreeMap<String, Node>();

        //вершина каналов (6 главных)
        Iterator iter = ChannelVideo.main_channels.iterator();
        while(iter.hasNext()){
            int x = rand.nextInt(3000);
            int y = rand.nextInt(1000);
            String node_id=iter.next().toString();
            Node n0 = graphModel.factory().newNode(node_id);
            n0.setLabel(node_id);
            n0.setSize(10);
            n0.setColor(Color.red);
            n0.setX(x);
            n0.setY(y);
            points.put(node_id, n0);
        }
        //перебираем все вершины и их соседей
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) iterator.next();
            String node_to = pair.getKey().toString();

            int x = rand.nextInt(3000);
            int y = rand.nextInt(1000);
            Node n0;
            if (!points.containsKey(node_to)) {
                n0 = graphModel.factory().newNode(node_to);
                n0.setLabel(node_to);
                n0.setSize(1);
                //n0.setColor(Color.red);
                n0.setX(x);
                n0.setY(y);
                points.put(node_to, n0);
            } else {
                n0 = points.get(node_to);
            }
            directedGraph.addNode(n0);
            //все вершины,ИЗ которых идет дуга на целевые
            Color c = new Color((int) (Math.random() * 0x1000000));
            Iterator it = Main.nodes_map.get(node_to).entrySet().iterator();
            int size = 0;
            //ссылающиеся вершины
            while (it.hasNext()) {

                Map.Entry pair2 = (Map.Entry) it.next();
                String node_from = pair2.getKey().toString();
                Node n2;
                if (!points.containsKey(node_from)) {
                    n2 = graphModel.factory().newNode(node_from);
                    n2.setLabel(node_from);
                    n2.setColor(Color.red);
                    n2.setSize(3);
                    int x1 = rand.nextInt(3000);
                    int y1 = rand.nextInt(1000);
                    n2.setX(x1);
                    n2.setY(y1);
                    points.put(node_from, n2);
                } else {
                    n2 = points.get(node_from);
                }
                //дуга
                Edge e1 = graphModel.factory().newEdge(n2, n0, 0, 1.0, true);
                e1.setWeight(0.1);

                if(Main.comment_count.containsKey(node_from+"!"+node_to)){
                  //  e1.setWeight(Main.comment_count.get(node_from+"!"+node_to));
                    e1.setLabel(String.valueOf(Main.comment_count.get(node_from+"!"+node_to)));
                }
                else if(Main.comment_count.containsKey(node_to+"!"+node_from)){
                   // e1.setWeight(Main.comment_count.get(node_to+"!"+node_from));
                    e1.setLabel(String.valueOf(Main.comment_count.get(node_to+"!"+node_from)));
                }
                else{
                    System.out.println("нет"+"node_from="+node_from+";node_to="+node_to);
                }
                //добавляем вершину
                directedGraph.addNode(n2);
                //добавляем ребро
                directedGraph.addEdge(e1);
            }

        }
        boolean end=true;
        if (degree > 1) {
            System.out.println("до:"+directedGraph.getNodeCount());
           do {
               end=true;
               System.out.println("end="+end);
                for (Node n : directedGraph.getNodes().toArray()) {
                    Node[] neighbors = directedGraph.getNeighbors(n).toArray();
                    if (neighbors.length < degree) {
                        directedGraph.removeNode(n);
                        end=false;
                    }
                }
            }
            while(!end);
            System.out.println("после:"+directedGraph.getNodeCount());
        }
    }

    //appearance(отображение графа)
    private static void filter(GraphModel graphModel,String type, Workspace workspace){

        DirectedGraph graph = graphModel.getDirectedGraph(); //получаем данные нашего графа по модели
        AppearanceController ac = Lookup.getDefault().lookup(AppearanceController.class); //создаем контроллер по отображению
        AppearanceModel appearanceModel = ac.getModel(workspace); // создаем модель
        //получаем список вершин(в нашем случае - отображение по вершинам) и определяем тип фильтрации
        Column centralityColumn = graphModel.getNodeTable().getColumn(type);
        //применяем функцию к graph - графу, по полученным вершинам - centralityColumn
        Function centralityRanking = appearanceModel.getNodeFunction(graph, centralityColumn, RankingNodeSizeTransformer.class);
        //объявляем объект по настройке трансформации и задаем настройки
        RankingNodeSizeTransformer centralityTransformer = centralityRanking.getTransformer();
        centralityTransformer.setMinSize(3);
        centralityTransformer.setMaxSize(20);
        //применяем изменение внешнего вида для нашей модели
        ac.transform(centralityRanking);
        //цвет
        Function centralityRanking2 = appearanceModel.getNodeFunction(graph, centralityColumn, RankingElementColorTransformer.class);
        RankingElementColorTransformer colorTransformer=(RankingElementColorTransformer)centralityRanking2.getTransformer();
        colorTransformer.setColors(new Color[]{Color.blue, Color.ORANGE,Color.cyan});
        ac.transform(centralityRanking2);

    }

    //модулярность графа (разбиение графа на сообщества)
    private static void modularity(GraphModel graphModel, Workspace workspace){
        DirectedGraph graph = graphModel.getDirectedGraph();
        Modularity modularity = new Modularity();
        modularity.execute(graphModel);
        AppearanceController ac = Lookup.getDefault().lookup(AppearanceController.class);
        AppearanceModel appearanceModel = ac.getModel(workspace);
        //Partition with 'modularity_class', just created by Modularity algorithm
        Column modColumn = graphModel.getNodeTable().getColumn(Modularity.MODULARITY_CLASS);
        Function func2 = appearanceModel.getNodeFunction(graph, modColumn, PartitionElementColorTransformer.class);
        Partition partition2 = ((PartitionFunction) func2).getPartition();
        System.out.println(partition2.size() + " partitions found");
        Palette palette2 = PaletteManager.getInstance().randomPalette(partition2.size());
        partition2.setColors(palette2.getColors());
        ac.transform(func2);
        System.out.println("node_count:"+directedGraph.getNodeCount());
    }

    //отображение
    private static void build(G2DTarget target){
        //создаем контроллер, отвечающий за отображение
        PreviewController previewController =
                Lookup.getDefault().lookup(PreviewController.class);
        PreviewModel previewModel = previewController.getModel();
        //настройки
        previewModel.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE); //отображение id вершин
        previewModel.getProperties().putValue(PreviewProperty.EDGE_LABEL_FONT,previewModel.getProperties().getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(8));
        previewModel.getProperties().putValue(PreviewProperty.SHOW_EDGE_LABELS, Boolean.TRUE); //отображение id вершин
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_PROPORTIONAL_SIZE, Boolean.FALSE);
        previewModel.getProperties().putValue(PreviewProperty.CATEGORY_NODE_LABELS, Boolean.TRUE);
        previewModel.getProperties().putValue(PreviewProperty.BACKGROUND_COLOR,Color.white);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_COLOR,
                new EdgeColor(Color.BLACK));

        target = (G2DTarget) previewController
                .getRenderTarget(RenderTarget.G2D_TARGET);
        //добавляем обработчик событий + paintComponent
        final PreviewScetch previewSketch = new PreviewScetch(target);
        previewController.render(target);
        previewController.refreshPreview();

        //отображение
        JFrame frame=new JFrame();
        frame.setLayout(new BorderLayout());
        //сюда накладываем тот объект, что отображается на окне
        frame.add(previewSketch, BorderLayout.CENTER);
        frame.setSize(600,700);

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                previewSketch.resetZoom();
            }
        });
        frame.setVisible(true);
        target.refresh();
        System.out.println("node_count:"+directedGraph.getNodeCount());
       // target.refresh();
        //previewSketch.resetZoom();

    }
}
