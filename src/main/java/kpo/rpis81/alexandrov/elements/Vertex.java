package kpo.rpis81.alexandrov.elements;

/**
 * Класс вершины графа со следующими полями:
 * {@link #parent} - порядковый номер родителя,
 * {@link #number} - собственный порядковый номер
 * @author Илья Александров
 */
public class Vertex implements Comparable<Vertex> {

    private int parent;
    private int number;

    public Vertex(int parent, int number) {
        this.parent = parent;
        this.number = number;
    }

    public int getParent() {
        return parent;
    }

    public void setParent(int parent) {
        this.parent = parent;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    @Override
    public String toString() {
        return number + "-" + parent;
    }

    @Override
    public int compareTo(Vertex v) {
        return number - v.getNumber();
    }
}
