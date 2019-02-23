/*
Licensed under MIT licence:

Copyright (C) 2019 Jiří Kyzlink <jkyzlink@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package ink.kyzl.lami.graphDemo;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.appearance.api.Function;
import org.gephi.appearance.api.Interpolator;
import org.gephi.appearance.api.Interpolator.BezierInterpolator;
import org.gephi.appearance.plugin.RankingElementColorTransformer;
import org.gephi.appearance.plugin.RankingNodeSizeTransformer;
import org.gephi.filters.api.FilterController;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.EdgeDirectionDefault;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.fruchterman.FruchtermanReingold;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.statistics.plugin.GraphDistance;
import org.openide.util.Lookup;

public class LamiDemo {    
    public void script(String inFile, String outFile) {
        //Init a project - and therefore a workspace
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        Workspace workspace = pc.getCurrentWorkspace();

        //Get models and controllers for this new workspace - will be useful later
        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
        ImportController importController = Lookup.getDefault().lookup(ImportController.class);
        FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
        AppearanceController appearanceController = Lookup.getDefault().lookup(AppearanceController.class);
        AppearanceModel appearanceModel = appearanceController.getModel();

        //Import file       
        Container container;
        try {
            File file = new File(inFile);
            container = importController.importFile(file);
            container.getLoader().setEdgeDefault(EdgeDirectionDefault.DIRECTED);   //Force DIRECTED
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        //Append imported data to GraphAPI
        importController.process(container, new DefaultProcessor(), workspace);

        //See if graph is well imported
        DirectedGraph graph = graphModel.getDirectedGraph();
        System.out.println("Nodes: " + graph.getNodeCount());
        System.out.println("Edges: " + graph.getEdgeCount());

        //Filter      
//        DegreeRangeFilter degreeFilter = new DegreeRangeFilter();
//        degreeFilter.init(graph);
//        degreeFilter.setRange(new Range(30, Integer.MAX_VALUE));     //Remove nodes with degree < 30
//        Query query = filterController.createQuery(degreeFilter);
//        GraphView view = filterController.filter(query);
//        graphModel.setVisibleView(view);    //Set the filter result as the visible view

        //See visible graph stats
//        UndirectedGraph graphVisible = graphModel.getUndirectedGraphVisible();
//        System.out.println("Nodes: " + graphVisible.getNodeCount());
//        System.out.println("Edges: " + graphVisible.getEdgeCount());

        //Run YifanHuLayout for 100 passes - The layout always takes the current visible view
        //This layout separates clusters and move them away from each other
        YifanHuLayout yifanLayout = new YifanHuLayout(null, new StepDisplacement(1f));
        yifanLayout.setGraphModel(graphModel);
        yifanLayout.resetPropertiesValues();
        yifanLayout.setOptimalDistance(40f);

        yifanLayout.initAlgo();
        for (int i = 0; i < 300 && yifanLayout.canAlgo(); i++)
            yifanLayout.goAlgo();
        yifanLayout.endAlgo();
        
        
        //Create circle-like layout
        FruchtermanReingold layout = new FruchtermanReingold(null);
        layout.setGraphModel(graphModel);
        layout.resetPropertiesValues();

        layout.initAlgo();
        for (int i = 0; i < 2000 && layout.canAlgo(); i++)
            layout.goAlgo();
        layout.endAlgo();
        
        
        //Adjust labels to be visible
//        LabelAdjust labelLayout = new LabelAdjust(null);
//        layout.setGraphModel(graphModel);
//        layout.resetPropertiesValues();
//        if(labelLayout.canAlgo())
//            labelLayout.goAlgo();
        
        
        //Get Centrality
        GraphDistance distance = new GraphDistance();
        distance.setDirected(true);
        distance.execute(graphModel);

        //Set spectrum-like colors to the graph edges based on weight
        Function ranking = appearanceModel.getEdgeFunction(graph, AppearanceModel.GraphFunction.EDGE_WEIGHT, RankingElementColorTransformer.class);
        RankingElementColorTransformer rankingTransformer = (RankingElementColorTransformer) ranking.getTransformer();
        int colorSteps = 5;
        Color[] colors = new Color[colorSteps];
        float[] colorsPos = new float[colorSteps];
        Interpolator intern = new BezierInterpolator(1.0f, 0.0f, 1.0f, 0.0f);
        //Create spectrum using the Hue component of the HSB color representation
        for(int i = 0; i<colorSteps; i++){
            colors[i]= Color.getHSBColor(((float)i/(colorSteps-1))*(240f/360), 1f, 1f);
            colorsPos[i] = intern.interpolate((float)i/(colorSteps-1));
        }
        rankingTransformer.setColors(colors);
        rankingTransformer.setColorPositions(colorsPos);
        appearanceController.transform(ranking);
        
        //Set node color by Degree
        Function nodeColor = appearanceModel.getNodeFunction(graph, AppearanceModel.GraphFunction.NODE_DEGREE, RankingElementColorTransformer.class);
        RankingElementColorTransformer nodeColorTransformer = (RankingElementColorTransformer) nodeColor.getTransformer();
        nodeColorTransformer.setColors(new Color[]{new Color(0x202020), new Color(0x808080)});
        nodeColorTransformer.setColorPositions(new float[]{0f, 1f});
        appearanceController.transform(nodeColor);

        //Rank size by centrality
        Column centralityColumn = graphModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);
        Function centralityRanking = appearanceModel.getNodeFunction(graph, centralityColumn, RankingNodeSizeTransformer.class);
        RankingNodeSizeTransformer centralityTransformer = (RankingNodeSizeTransformer) centralityRanking.getTransformer();
        centralityTransformer.setMinSize(5);
        centralityTransformer.setMaxSize(10);
        appearanceController.transform(centralityRanking);

        //Preview - set parameters so the graph will look better
        model.getProperties().putValue(PreviewProperty.EDGE_RESCALE_WEIGHT, Boolean.TRUE);
        model.getProperties().putValue(PreviewProperty.EDGE_RESCALE_WEIGHT_MIN, 2.0f);
        model.getProperties().putValue(PreviewProperty.EDGE_RESCALE_WEIGHT_MAX, 10.0f);
        model.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(EdgeColor.Mode.ORIGINAL));
        model.getProperties().putValue(PreviewProperty.EDGE_CURVED, Boolean.FALSE);
        model.getProperties().putValue(PreviewProperty.NODE_OPACITY, 30f);
        model.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
        model.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, model.getProperties().getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(20));
        model.getProperties().putValue(PreviewProperty.NODE_LABEL_PROPORTIONAL_SIZE, Boolean.FALSE);

        //Export
        Export(outFile);
    }
    
    public void Export(String path){
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        try {
            ec.exportFile(new File(path));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
