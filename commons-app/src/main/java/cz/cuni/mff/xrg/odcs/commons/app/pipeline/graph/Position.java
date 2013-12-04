/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.xrg.odcs.commons.app.pipeline.graph;

import cz.cuni.mff.xrg.odcs.commons.app.dao.DataObject;
import java.awt.Point;
import javax.persistence.*;

/**
 * Represent coordinates of object in system (on canvas).
 *
 * @author Jiri Tomes
 */
@Entity
@Table(name = "ppl_position")
public class Position implements DataObject {

	/**
	 * Primary key of graph stored in db
	 */
	@Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_ppl_position")
	@SequenceGenerator(name = "seq_ppl_position", allocationSize = 1)
	private Long id;

	/**
	 * X coordinate in pixels
	 */
	@Column(name = "pos_x")
	private int x;

	/**
	 * Y coordinate in pixels
	 */
	@Column(name = "pos_y")
	private int y;

	/**
	 * No-arg constructor for JPA
	 */
	public Position() {
	}

	public Position(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Copy constructor.
	 * 
	 * @param position 
	 */
	public Position(Position position) {
		x = position.x;
		y = position.y;
	}

	public void changePosition(int newX, int newY) {
		x = newX;
		y = newY;
	}

	public Point getPositionAsPoint() {
		Point positionPoint = new Point(x, y);
		return positionPoint;
	}

	public int getX() {
		return x;
	}

	public void setX(int newX) {
		x = newX;
	}

	public int getY() {
		return y;
	}

	public void setY(int newY) {
		y = newY;
	}

	@Override
	public Long getId() {
		return id;
	}
}
