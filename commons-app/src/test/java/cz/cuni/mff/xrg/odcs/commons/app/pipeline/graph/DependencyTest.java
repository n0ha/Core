package cz.cuni.mff.xrg.odcs.commons.app.pipeline.graph;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * Test suite for testing pipeline graph dependency resolving algorithms.
 * 
 * @author Jan Vojt
 */
public class DependencyTest {

    /**
     * Nodes in pipeline graph created by factory {@link #buildGraph(int)}.
     */
    private Node[] nodes;

    /**
     * Test dependency resolution for serialized DPURecord setup.
     * Scenario: E -> T -> T -> T -> L
     */
    @Test
    public void testInlineDependencyResolution() {

        PipelineGraph graph = buildGraph(5);
        graph.addEdge(nodes[2], nodes[3]);
        graph.addEdge(nodes[0], nodes[1]);
        graph.addEdge(nodes[1], nodes[2]);
        graph.addEdge(nodes[3], nodes[4]);

        DependencyGraph dGraph = new DependencyGraph(graph);

        GraphIterator iter = dGraph.iterator();

        // check correct order
        for (int i = 0; i < 5; i++) {
            assertTrue(iter.hasNext());
            assertSame(nodes[i], iter.next());
        }

        // no more nodes
        assertFalse(iter.hasNext());
        assertNull(iter.next());
    }

    /**
     * Test more complex setup of DPUs.
     * Scenario: E0 -> T1 -> T2 -> L3
     * E4 ---^
     */
    @Test
    public void testComplexDependencyResolution() {

        PipelineGraph graph = buildGraph(5);
        graph.addEdge(nodes[0], nodes[1]);
        graph.addEdge(nodes[1], nodes[2]);
        graph.addEdge(nodes[2], nodes[3]);
        graph.addEdge(nodes[4], nodes[2]);

        DependencyGraph dGraph = new DependencyGraph(graph);

        GraphIterator iter = dGraph.iterator();

        // first must be E0 or E4
        Node n = iter.next();
        assertTrue(n == nodes[0] || n == nodes[4]);

        // second may be any of E0, E4, T1
        n = iter.next();
        assertTrue(n == nodes[0] || n == nodes[1]
                || n == nodes[4]);

        // third may be E4 or T1
        n = iter.next();
        assertTrue(n == nodes[1] || n == nodes[4]);

        // fourth is always T2
        n = iter.next();
        assertSame(nodes[2], n);

        // last is always L3
        n = iter.next();
        assertSame(nodes[3], n);

        // no more nodes
        assertFalse(iter.hasNext());
        assertNull(iter.next());
    }

    /**
     * Test circular dependency resolution.
     * Scenario: E -> T -> T -> T -> T
     * ^---------'
     */
    @Test
    public void testCircularDependencyResolution() {

        PipelineGraph graph = buildGraph(5);
        graph.addEdge(nodes[0], nodes[1]);
        graph.addEdge(nodes[1], nodes[2]);
        graph.addEdge(nodes[2], nodes[3]);
        graph.addEdge(nodes[3], nodes[4]);
        graph.addEdge(nodes[3], nodes[1]);

        DependencyGraph dGraph = new DependencyGraph(graph);
        GraphIterator iter = dGraph.iterator();

        // first node is not in the circle
        assertTrue(iter.hasNext());
        assertSame(nodes[0], iter.next());

        // second node is in the circle
        // graph still says to have more nodes and so return true for hasNext,
        // however does not return any node, because it is impossible to tell
        // which node is next
        assertTrue(iter.hasNext());
        assertNull(iter.next());
    }

    /**
     * Test debugging till given DPU instance in a graph with a single node.
     * !
     * Scenario: E -> L
     */
    @Test
    public void testDebugNodeInTrivialGraph() {

        PipelineGraph graph = buildGraph(5);
        graph.addEdge(nodes[0], nodes[1]);

        DependencyGraph dGraph = new DependencyGraph(graph, nodes[0]);
        GraphIterator iter = dGraph.iterator();

        assertTrue(iter.hasNext());
        assertEquals(nodes[0], iter.next());

        assertFalse(iter.hasNext());
        assertNull(iter.next());
    }

    /**
     * Test debugging till given DPU instance in a more complex graph.
     * !
     * Scenario: E0 -> T1 -> T2 -> L3
     * E4 -> T5 ---^ E6 -^
     */
    @Test
    public void testDebugNodeInComplexGraph() {

        PipelineGraph graph = buildGraph(7);
        graph.addEdge(nodes[0], nodes[1]);
        graph.addEdge(nodes[1], nodes[2]);
        graph.addEdge(nodes[2], nodes[3]);
        graph.addEdge(nodes[4], nodes[5]);
        graph.addEdge(nodes[5], nodes[1]);
        graph.addEdge(nodes[6], nodes[2]);

        DependencyGraph dGraph = new DependencyGraph(graph, nodes[1]);
        GraphIterator iter = dGraph.iterator();

        Set<Node> nodesToRun = new HashSet<>();
        nodesToRun.add(nodes[0]);
        nodesToRun.add(nodes[1]);
        nodesToRun.add(nodes[4]);
        nodesToRun.add(nodes[5]);

        for (int i = 0; i < 4; i++) {
            assertTrue(iter.hasNext());
            assertTrue(nodesToRun.contains(iter.next()));
        }

        assertFalse(iter.hasNext());
        assertNull(iter.next());
    }

    /**
     * Pipeline graph helper factory.
     * 
     * @param size
     *            number of nodes in graph
     * @return
     */
    private PipelineGraph buildGraph(int size) {

        PipelineGraph graph = new PipelineGraph();

        nodes = new Node[size];
        for (int i = 0; i < size; i++) {
            Node node = new Node();
            nodes[i] = node;
            graph.addNode(node);
        }

        return graph;
    }
}
