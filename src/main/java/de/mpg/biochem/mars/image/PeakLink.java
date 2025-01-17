/*-
 * #%L
 * Molecule Archive Suite (Mars) - core data storage and processing algorithms.
 * %%
 * Copyright (C) 2018 - 2025 Karl Duderstadt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package de.mpg.biochem.mars.image;

/**
 * PeakLinks are used to link peaks over time during peak tracking. PeakLinks
 * store the start and end peaks that are linked as well as time, difference in time and square
 * distance.
 * 
 * @author Karl Duderstadt
 */
public class PeakLink {

	private Peak from;
	private long fromID = -1;
	private Peak to;
	private long toID = -1;
	private double distanceSq;
	private int t;
	private int tDifference;

	public PeakLink(Peak from, Peak to, double distanceSq, int t,
		int tDifference)
	{
		this.from = from;
		this.to = to;
		this.distanceSq = distanceSq;
		this.t = t;
		this.tDifference = tDifference;
	}

	@SuppressWarnings("unused")
	public PeakLink(long fromID, long toID, double distanceSq, int t,
					int tDifference)
	{
		this.fromID = fromID;
		this.toID = toID;
		this.distanceSq = distanceSq;
		this.t = t;
		this.tDifference = tDifference;
	}

	@SuppressWarnings("unused")
	public void reset(Peak from, Peak to, double distanceSq, int t,
					  int tDifference)
	{
		this.from = from;
		this.to = to;
		this.distanceSq = distanceSq;
		this.t = t;
		this.tDifference = tDifference;
	}

	@SuppressWarnings("unused")
	public void reset(long fromID, long toID, double distanceSq, int t,
					  int tDifference)
	{
		this.fromID = fromID;
		this.toID = toID;
		this.distanceSq = distanceSq;
		this.t = t;
		this.tDifference = tDifference;
	}

	public double getSquaredDistance() {
		return distanceSq;
	}

	public void setSquaredDistance(double distanceSq) {
		this.distanceSq = distanceSq;
	}

	public void setT(int t) {
		this.t = t;
	}

	public int getT() {
		return t;
	}

	public void setTDifference(int tDifference) {
		this.tDifference = tDifference;
	}

	public int getTDifference() {
		return tDifference;
	}

	public Peak getFrom() {
		return from;
	}

	public long getFromID() {
		return fromID;
	}

	public Peak getTo() {
		return to;
	}

	public long getToID() {
		return toID;
	}
}
