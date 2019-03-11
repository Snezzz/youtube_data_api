import org.apache.commons.collections4.list.TreeList;
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
import org.gephi.filters.api.FilterController;
import org.gephi.filters.plugin.graph.EgoBuilder;
import org.gephi.graph.api.*;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.spi.GraphExporter;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.openord.OpenOrdLayout;
import org.gephi.preview.api.*;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.Workspace;
import org.gephi.statistics.plugin.*;
import org.openide.util.Lookup;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class GraphCreater extends JFrame {


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
    private static String type;
    private static Map<String,Double> top;
    private static Map<String,Color> colors;


    public GraphCreater(int n, String title, boolean Default, boolean betweeness, boolean pagerank, boolean module,
                        G2DTarget target) throws IOException, CloneNotSupportedException {
        super(title);

        //создание нового проекта
        GetData.pc.newProject();
        workspace = GetData.pc.getCurrentWorkspace();

        //модель графа
        GraphModel graphModel = Lookup.getDefault()
                .lookup(GraphController.class).getGraphModel();
        directedGraph = graphModel.getDirectedGraph();
        //создание узлов и ребер на directedGraph
        create(n,graphModel,directedGraph,points);
       // test_create(graphModel);

        //анализ
        get_analysis(graphModel);
        //write_results();

        //укладка графа по заданному алгоритму
        stowage("OpenOrd",graphModel);
      // stowage("YifanHu",graphModel);

        //BETWEENESS_CENTRALITY
        if(betweeness) {
            filter(graphModel, GraphDistance.BETWEENNESS, workspace);
            type = "Betweeness";
        }
       else if(pagerank) {
            //PageRank
            filter(graphModel, PageRank.PAGERANK, workspace);
            type = "PageRank";
        }
        //модулярность
       else if(module) {
         //   modularity(graphModel, workspace);
           new_graph(graphModel);
            type = "Modularity";
        }
        //отображение графа на панели
        build(target);
        export();
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

        //кратчайшие пути
        GraphDistance distance = new GraphDistance();
        distance.setDirected(true);
        distance.setNormalized(true);
        distance.execute(graphModel);



        //System.out.println("Диаметр графа: "+distance.getDiameter());
        //System.out.println("Радиус графа: "+distance.getRadius());

        //System.out.println("Центральность графа: ");
        //System.out.println("1.По посредничеству:");
       /* for (Node n : directedGraph.getNodes()) {
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
        */
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
      /*  for (Node n : directedGraph.getNodes()) {
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
        */


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
        File myFile_ED = new File("results/data_result.xls");
        FileOutputStream outputStream = new FileOutputStream(myFile_ED);
        //FileInputStream inputStream_ED = new FileInputStream(myFile_ED);

        HSSFWorkbook workbook = new HSSFWorkbook();

        create_XLSlist(workbook,"betweeness_centrality",betweeness_centrality);
        create_XLSlist(workbook,"closeness_centrality",closeness_centrality);
        create_XLSlist(workbook,"eigenvector_centrality",eigenvector_centrality);
        create_XLSlist(workbook,"ECCENTRICITY",ECCENTRICITY);
        create_XLSlist(workbook,"PAGERANK",PAGERANK);
        create_XLSlist(workbook,"authority",authority);
        create_XLSlist(workbook,"hub",hub);
        create_XLSlist(workbook,"WEIGHTEDDEGREE",WEIGHTEDDEGREE);
        create_XLSlist(workbook,"DEGREE",DEGREE);
        workbook.write(outputStream);
        outputStream.close();
/*
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
*/
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
        double count = Math.pow(directedGraph.getNodes().toArray().length,2);
        if (type.equals("YifanHu")) {
            //YifanHu укладка
            YifanHuLayout layout = new YifanHuLayout(null, new StepDisplacement(1f));
            layout.setGraphModel(graphModel);
            layout.resetPropertiesValues();
            layout.setOptimalDistance(13593.233f);
          //  layout.setRelativeStrength(3000f);
            layout.initAlgo();
            for (int i = 0; i < 100 && layout.canAlgo(); i++) {
                layout.goAlgo();
            }


        } else if(type.equals("OpenOrd")) {
            OpenOrdLayout layout = new OpenOrdLayout(null);
            layout.setGraphModel(graphModel);
            layout.resetPropertiesValues();
            layout.setCooldownStage(100);
            layout.setCrunchStage(0);
            layout.setExpansionStage(1);
            layout.setLiquidStage(2);
            layout.setSimmerStage(1);
            layout.setEdgeCut(1.0f);
            layout.setNumThreads(8);
            layout.initAlgo();
            for (int i = 0; i < 100 && layout.canAlgo(); i++) {
                layout.goAlgo();
            }
        }
    }

    private static void test_create( GraphModel graphModel){
        directedGraph = graphModel.getDirectedGraph();
        Node n1 = graphModel.factory().newNode("1");
        n1.setX(10);
        n1.setY(20);
        n1.setSize(1);
        n1.setLabel("n1");
        Node n2 = graphModel.factory().newNode("2");
        n2.setX(20);
        n2.setY(20);
        n2.setSize(1);
        n2.setLabel("n2");
        Node n3 = graphModel.factory().newNode("3");
        n3.setX(10);
        n3.setY(40);
        n3.setSize(1);
        n3.setLabel("n3");
        Edge e1 = graphModel.factory().newEdge(n2, n1, 1, 0.1, true);
        Edge e2 = graphModel.factory().newEdge(n3, n1, 1, 0.1, true);
        e2.setWeight(30);
        e1.setWeight(10);
        directedGraph.addNode(n2);
        directedGraph.addNode(n1);
        directedGraph.addEdge(e1);
        directedGraph.addNode(n3);
        directedGraph.addEdge(e2);
    }
    //создание графа
    private static void create(int degree, GraphModel graphModel,DirectedGraph directedGraph, Map<String,Node> points) {
        Random rand = new Random();
       // Iterator iterator = SetData3.nodes_map.entrySet().iterator();
        Iterator iterator = GetData.nodes.entrySet().iterator();
        colors = new LinkedHashMap<String, Color>();
        directedGraph = graphModel.getDirectedGraph();
         points = new TreeMap<String, Node>();

        //вершина каналов (6 главных)
        /*Iterator iter = ChannelVideo.main_channels.iterator();
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
        */
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
                n0.setSize(6);
               n0.setColor(Color.red);
                n0.setX(x);
                n0.setY(y);
                points.put(node_to, n0);
            } else {
                n0 = points.get(node_to);
            }
            directedGraph.addNode(n0);
            //все вершины,ИЗ которых идет дуга на целевые
            Color c = new Color((int) (Math.random() * 0x1000000));
           // Iterator it = SetData3.nodes_map.get(node_to).entrySet().iterator();
            Iterator it = GetData.nodes.get(node_to).entrySet().iterator();

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
                    n2.setSize(6);
                    int x1 = rand.nextInt(3000);
                    int y1 = rand.nextInt(1000);
                    n2.setX(x1);
                    n2.setY(y1);
                    points.put(node_from, n2);
                } else {
                    n2 = points.get(node_from);
                }
                if(n2!=n0) {
                    //дуга
                    Edge e1 = graphModel.factory().newEdge(n2, n0, 0, 1.0, true);
                    //e1.setLabel(pair2.getValue().toString());

                    //вес ребра
                    if (GetData.comments_count.containsKey(node_from + "!" + node_to)) {
                        e1.setWeight(GetData.comments_count.get(node_from + "!" + node_to));
                        e1.setLabel(String.valueOf(GetData.comments_count.get(node_from + "!" + node_to)));
                    } else if (GetData.comments_count.containsKey(node_to + "!" + node_from)) {
                        e1.setWeight(GetData.comments_count.get(node_to + "!" + node_from));
                        e1.setLabel(String.valueOf(GetData.comments_count.get(node_to + "!" + node_from)));
                    } else {
                        //   System.out.println("нет"+"node_from="+node_from+";node_to="+node_to);
                    }
                    //добавляем вершину
                    directedGraph.addNode(n2);
                    //добавляем ребро
                    directedGraph.addEdge(e1);
                }
            }

        }
        boolean end=true;

       if (degree > 1) {
            System.out.println("до:"+directedGraph.getNodeCount());
           do {
               end=true;
             //  System.out.println("end="+end);
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

        //получаем данные нашего графа по модели
        DirectedGraph graph = graphModel.getDirectedGraph();

        AppearanceController ac = Lookup.getDefault().lookup(AppearanceController.class); //создаем контроллер по отображению
        AppearanceModel appearanceModel = ac.getModel(workspace); // создаем модель
        //получаем список вершин(в нашем случае - отображение по вершинам) и определяем тип фильтрации
        Column centralityColumn = graphModel.getNodeTable().getColumn(type);

        Node [] nodes = graph.getNodes().toArray();
        top = find_top(nodes,5,centralityColumn);
        //применяем функцию к graph - графу, по полученным вершинам - centralityColumn
        //размер вершин
        Function centralityRanking = appearanceModel.getNodeFunction(graph, centralityColumn, RankingNodeSizeTransformer.class);
        //объявляем объект по настройке трансформации и задаем настройки
        RankingNodeSizeTransformer centralityTransformer = centralityRanking.getTransformer();
        centralityTransformer.setMinSize(1);
        centralityTransformer.setMaxSize(20);
        //применяем изменение внешнего вида для нашей модели
        ac.transform(centralityRanking);


          //цвет вершин
        Function centralityRanking2 = appearanceModel.getNodeFunction(graph, centralityColumn, RankingElementColorTransformer.class);
        RankingElementColorTransformer colorTransformer=(RankingElementColorTransformer)centralityRanking2.getTransformer();
       // colorTransformer.setColors(new Color[]{Color.GRAY, Color.ORANGE,Color.red});
        colorTransformer.setColors(new Color[]{Color.pink,Color.YELLOW, Color.orange,Color.red});

        ac.transform(centralityRanking2);
        System.out.println("");

    }

    //модулярность графа (разбиение графа на сообщества)
    private static void modularity(GraphModel graphModel, Workspace workspace){

        DirectedGraph graph = graphModel.getDirectedGraph();

        Modularity modularity = new Modularity();
        modularity.setUseWeight(true);
        modularity.setRandom(true);
        modularity.setResolution(1.0);
        modularity.execute(graphModel);

        //получили кластеры
        //отображение
        AppearanceController ac = Lookup.getDefault().lookup(AppearanceController.class);
        AppearanceModel appearanceModel = ac.getModel(workspace);
        //Partition with 'modularity_class', just created by Modularity algorithm
         //цвет вершин
        Column modColumn = graphModel.getNodeTable().getColumn(Modularity.MODULARITY_CLASS);
        Function func2 = appearanceModel.getNodeFunction(graph, modColumn, PartitionElementColorTransformer.class);

        //Node [] nodes = graph.getNodes().toArray();
       // top = find_top(nodes,5,modColumn);

        Partition partition2 = ((PartitionFunction) func2).getPartition();

        System.out.println(partition2.size() + " partitions found");
        //настройка цветов вершин
        Palette palette2 = PaletteManager.getInstance().randomPalette(partition2.size());
        partition2.setColors(palette2.getColors());
        top = find_partitions(partition2);
        ac.transform(func2);

        Function func = appearanceModel.getEdgeFunction(graph, AppearanceModel.GraphFunction.EDGE_WEIGHT
                ,RankingElementColorTransformer.class);

        ac.transform(func);
        Node [] nodes = graph.getNodes().toArray();
        Map <String, Node> map = new HashMap<String, Node>();
        Iterator it = graph.getNodes().iterator();
        for(int i = 0; i< nodes.length; i++){
            map.put(nodes[i].getId().toString(),nodes[i]);
        }
        List <String> to_delete = new TreeList<String>();
        Map <String, String> edges = new TreeMap<String, String>();
        Node [] new_nodes = nodes.clone();
        System.out.println("node_count_before:"+map.size());
        directedGraph = graphModel.getDirectedGraph();
        directedGraph.readUnlock();
        for(int i = 0; i< nodes.length; i++){
            String [] str = nodes[i].getAttributes()[0].toString().split("");
            if(str.length == 24) {
                String current_id = nodes[i].getAttributes()[0].toString();
                for (Map.Entry<String, Node> entry : map.entrySet()) {
                    String [] strings = entry.getKey().split("");
                    if(strings.length > 24) {
                        String find_id = entry.getKey().toString().substring(0, 24);
                        if (find_id.equals(current_id)) {
                           Edge [] edge =  graph.getEdges(entry.getValue()).toArray();
                           Collection <Edge> edges1 = graph.getEdges(entry.getValue()).toCollection();
                           directedGraph.addAllEdges(put_edges(graphModel,edge,nodes[i]));
                            directedGraph.removeAllEdges(edges1);
                           to_delete.add(entry.getKey());
                        }
                    }
                }
            }
        }

        NodeIterable n = graphModel.getDirectedGraph().getNodes();
        Node [] node = n.toArray();
        List <Node> nodes1 = new TreeList<Node>();

        for(int i = 0; i< node.length; i++){
            if(to_delete.contains(node[i].getId().toString())) {
               directedGraph.removeNode(node[i]);
            }
         }

        System.out.println("node_count_after:"+directedGraph.getNodes().toArray().length);
    }
    private static Collection<Edge> put_edges(GraphModel graph,Edge [] edges,Node target){
        Edge new_edge;
        Collection <Edge> new_edges = new HashSet<Edge>();
        for(int i=0;i<edges.length;i++){
            Node sourse = edges[i].getSource();
            Double weight = edges[i].getWeight();
            //если в точку, что мы удаляем (target)
            if(edges[i].getTarget().getId()!=target.getId()){
                new_edge = graph.factory().newEdge(sourse,target, 1, weight, true);
            }
            //если из точки, которую мы удаляем
            else{
                Node new_node = edges[i].getTarget();
                new_edge = graph.factory().newEdge(target,new_node, 1, weight, true);
            }
            new_edges.add(new_edge);
           // directedGraph.readUnlock();
             //   directedGraph.addEdge(new_edge);

        }
        return new_edges;
    }
    //отображение
    private static void build(G2DTarget target){
        //создаем контроллер, отвечающий за отображение
        PreviewController previewController =
                Lookup.getDefault().lookup(PreviewController.class);
        PreviewModel previewModel = previewController.getModel();

        //настройки
     //   previewModel.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS,Boolean.TRUE);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_LABEL_FONT,previewModel.getProperties().getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(8));
        previewModel.getProperties().putValue(PreviewProperty.SHOW_EDGE_LABELS, Boolean.TRUE); //отображение id вершин
        previewModel.getProperties().putValue(PreviewProperty.EDGE_LABEL_FONT,new Font("Times New Roman",2,2));
        previewModel.getProperties().putValue(PreviewProperty.DIRECTED,
                Boolean.TRUE);
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_PROPORTIONAL_SIZE, Boolean.FALSE);
        previewModel.getProperties().putValue(PreviewProperty.CATEGORY_NODE_LABELS, Boolean.TRUE);
        previewModel.getProperties().putValue(PreviewProperty.BACKGROUND_COLOR,Color.white);
       previewModel.getProperties().putValue(PreviewProperty.EDGE_COLOR,
                    new EdgeColor(EdgeColor.Mode.MIXED));
        previewModel.getProperties().putValue(PreviewProperty.EDGE_RESCALE_WEIGHT,Boolean.TRUE);
        previewModel.getProperties().putValue(PreviewProperty.SHOW_EDGE_LABELS, Boolean.FALSE);

        previewModel.getProperties().putValue(PreviewProperty.EDGE_CURVED,Boolean.FALSE);
        previewModel.getProperties().putValue(PreviewProperty.ARROW_SIZE,0.01);
       // previewModel.getProperties().putValue(PreviewProperty.EDGE_OPACITY, 50);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_THICKNESS, 1.0f);
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_PROPORTIONAL_SIZE,6);

        target = (G2DTarget) previewController
                .getRenderTarget(RenderTarget.G2D_TARGET);
        //добавляем обработчик событий + paintComponent
        final PreviewScetch previewSketch = new PreviewScetch(target);
        previewController.render(target);
        previewController.refreshPreview();
        //отображение
        JFrame frame=new JFrame(type);
        frame.setLayout(new BorderLayout());
        //сюда накладываем тот объект, что отображается на окне
        frame.add(previewSketch, BorderLayout.CENTER);
        frame.setSize(1000,700);

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                previewSketch.resetZoom();
            }
        });
        JFrame help = new JFrame("");
        help.setLocation(600,100);
        help.setSize(300,200);
        int top_size = top.size();

        JPanel panel = new JPanel();
        panel.setSize(100,500);
        panel.setBounds(600,100,100,300);

        JTable table = create_table(top_size,type);
        for(int i=0;i<table.getColumnCount();i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(150);
        }

        panel.add(((JTable) table).getTableHeader(), BorderLayout.NORTH);
        panel.add(table, BorderLayout.CENTER);
        help.add(panel);

        //frame.getContentPane().add(help);
        frame.setVisible(true);
        help.setVisible(true);
        target.refresh();
        System.out.println("node_count:"+directedGraph.getNodeCount());
    }

    //поиск топ-показателей
    private static  Map<String,Double> find_top(Node [] nodes,int howMany,Column column){
        Map<String,Double> results = new LinkedHashMap<String, Double>();
        Node [] new_nodes = new Node[nodes.length];
        double max = 0;
        for(int i=0;i<nodes.length;i++){
            for(int j=i+1;j<nodes.length-1;j++) {
                if (Double.valueOf(nodes[j].getAttribute(column).toString())
                        > Double.valueOf(nodes[i].getAttribute(column).toString())) {
                    Node node = nodes[i];
                    nodes[i]=nodes[j];
                    nodes[j] = node;
                }
            }
        }
        for(int i=0;i<howMany;i++){
            results.put(nodes[i].getAttributes()[0].toString(),
                    Double.valueOf(nodes[i].getAttribute(column).toString()));
         //   colors.put(nodes[i].getAttributes()[0].toString(),nodes[i].getColor());
        }
        return results;
    }

    private static Map<String,Double> find_partitions(Partition partition){
        Map<String,Double> result = new LinkedHashMap<String, Double>();

        Collection collection = partition.getSortedValues();
       Iterator it = collection.iterator();
       while(it.hasNext()) {
        Object value = it.next();
         double perc =  (double) partition.percentage(value);
         result.put(value.toString(),perc);
         colors.put(value.toString(),partition.getColor(value));
       }
            return result ;
    }
    private static JTable create_table(int size,String type){
        JTable result = null;
        String[] header = new String[2];
        Object[][] data =  new Object[size][header.length];
        if(type.equals("Betweeness")||(type.equals("PageRank"))) {
            header = new String[2];
            header[0] = "UserId";
            header[1] = "Value";
            int i = 0;
            for (Map.Entry<String, Double> entry : top.entrySet()) {
                data[i][0] = entry.getKey().toString();
                data[i][1] = entry.getValue().toString();
                i++;
            }
            result = new JTable(data,header);
        }
        else if(type.equals("Modularity")){
            header[0] = "GroapId";
          //  header[1] = "Color";
            header[1] = "User percentage(%)";
            int i = 0;
            for (Map.Entry<String, Double> entry : top.entrySet()) {
                data[i][0] = entry.getKey().toString();
                data[i][1] = entry.getValue().toString();
                i++;
            }
            result = new JTable(data,header);
            result.setDefaultRenderer(Object.class, new TableInfoRenderer());
        }


        //result.getColumnModel().getColumn(0).setCellRenderer(renderer);
        return result;
    }
    public static class TableInfoRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            Font font = new Font("Century Gothic", Font.BOLD,14);
                c.setBackground(colors.get(value));
                c.setFont(font);
            return c;
        }
    }
    private static void export() {
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        try {
            ec.exportFile(new File("test - full.gexf"));
        } catch
                (IOException ex) {
            ex.printStackTrace();
            return;
        }
        //Export only visible
       GraphExporter exporter = (GraphExporter) ec.getExporter("gexf");
        //Get GEXF
       exporter.setExportVisible(true);
        //Only exports the visible (ltered) graph
        try {
            ec.exportFile(new File("test - visible.gexf"), exporter);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
    }

    private static void new_graph(GraphModel graphModel) throws CloneNotSupportedException {
       // GraphModel new_model = graphModel;
        //GetData.pc.newProject();
       //Workspace new_workspace =
        //Lookup.EMPTY.lookup(GraphController.class).getGraphModel();
        GraphModel new_model = Lookup.getDefault()
                .lookup(GraphController.class).getGraphModel();
        DirectedGraph graph = graphModel.getDirectedGraph();
        DirectedGraph graphP = graph;
     //   Collection old_nodes = graph.getNodes().toCollection();
       // Collection edges = graph.getEdges().toCollection();
        //graphP.addAllNodes(old_nodes);
        //graphP.addAllEdges(edges);
        List<Node> persona = new TreeList<Node>();

        FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
        Modularity modularity = new Modularity();
        modularity.setUseWeight(true);
        modularity.setRandom(true);
        modularity.setResolution(1.0);
        modularity.execute(graphModel);
        EgoBuilder.EgoFilter egoFilter = new EgoBuilder.EgoFilter();
        Node [] nodes = graph.getNodes().toArray();
        egoFilter.setDepth(1);

        for(int i=0; i < nodes.length; i++){
            String node_id = nodes[i].getId().toString();

        //    egoFilter.setPattern(node_id);
          //  Query queryEgo = filterController.createQuery(egoFilter);
           // GraphView viewEgo = filterController.filter(queryEgo);
            //graphModel.setVisibleView(viewEgo);    //Set the filter result as the visible view
//Count nodes and edges on filtered graph
            //graph = graphModel.getDirectedGraphVisible();
            Node [] neighbours = graph.getNeighbors(nodes[i]).toArray();
           // Node [] neighbours = graph.getNodes().toArray();
            Column modColumn = graphModel.getNodeTable().getColumn(Modularity.MODULARITY_CLASS);
            String current_community = nodes[i].getAttribute(modColumn).toString();
            for( int j = 0; j < neighbours.length; j++){
                if (neighbours[j].getId().toString().equals(node_id))
                    continue;
                String community = neighbours[j].getAttribute(modColumn).toString();
                if(!current_community.equals(community)){
                    Node new_node = graphModel.factory().newNode(nodes[i].getId()+""+j);
                    // new_node.setAttribute()nodes[i];

                    graphP.addNode(new_node);
                    //старое ребро
                    Edge edge = graph.getEdge(nodes[i],neighbours[j]);
                    if(edge==null){
                        edge = graph.getEdge(neighbours[j],nodes[i]);
                    }
                    double weight = edge.getWeight();
                    //удаляем из предыдущей вершины
                    graphP.removeEdge(edge);
                    //новое ребро
                    Edge new_edge = graphModel.factory().newEdge(new_node, neighbours[j], 1, weight, true);
                    graphP = graphModel.getDirectedGraph();
                    graphP.addEdge(new_edge);
                    new_node.removeAttribute(modColumn);
                  //  new_node.setAttribute("id",nodes[i].getId());
                    new_node.setAttribute(Modularity.MODULARITY_CLASS,Integer.valueOf(community));
                    persona.add(new_node);
                }
            }
            //список вершин
            persona.add(nodes[i]);

         //   System.out.println("Nodes: " + graph.getNodeCount() + " Edges: " + graph.getEdgeCount());
        }
        System.out.println("Nodes: " + graph.getNodeCount() + " Edges: " + graph.getEdgeCount());

        modularity(graphModel,workspace);
         }
    public GraphModel clone() throws CloneNotSupportedException{

        return (GraphModel) super.clone();
    }

}
