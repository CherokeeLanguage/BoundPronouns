package com.cherokeelessons.bp.desktop;

enum ScreenSize {
	_4s(960,640), _5(1136,640), _6(1280,720), _6p(1280,720);
	private final int width;
	private final int height;
	private ScreenSize(int width, int height) {
		this.width=width;
		this.height=height;
	}
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
}