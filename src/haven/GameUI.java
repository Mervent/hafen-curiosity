/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.rx.BuffToggles;
import haven.rx.Reactor;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.*;
import java.util.List;

import static haven.Action.*;
import static haven.Inventory.*;
import static haven.ItemFilter.*;
import static haven.KeyBinder.*;

public class GameUI extends ConsoleHost implements Console.Directory {
    public static final Text.Foundry msgfoundry = new Text.Foundry(Text.dfont, 14);
    private static final int blpw = 142, brpw = 142;
    public final String chrid, genus;
    public final long plid;
    private final Hidepanel ulpanel, umpanel, urpanel, blpanel, brpanel, menupanel;
    public Avaview portrait;
    public MenuGrid menu;
    public MapView map;
    public LocalMiniMap mmap;
    public Fightview fv;
    private List<Widget> meters = new LinkedList<Widget>();
    private List<Widget> cmeters = new LinkedList<Widget>();
    private Text lastmsg;
    private double msgtime;
    private Window invwnd, equwnd;
    private CraftWindow makewnd;
    public Inventory maininv;
    public Equipory equipory;
    public CharWnd chrwdg;
    public MapWnd mapfile;
    private Widget qqview;
    public BuddyWnd buddies;
    public EquipProxy eqproxy;
    public FilterWnd filter;
    public Cal calendar;
    private final Zergwnd zerg;
    public final Collection<Polity> polities = new ArrayList<Polity>();
    public HelpWnd help;
    public OptWnd opts;
    public Collection<DraggedItem> hand = new LinkedList<DraggedItem>();
    private Collection<DraggedItem> handSave = new LinkedList<DraggedItem>();
    private WItem vhand;
    public ChatUI chat;
    public ChatUI.Channel syslog;
    public double prog = -1;
    private boolean afk = false;
    @SuppressWarnings("unchecked")
    public Indir<Resource>[] belt = new Indir[144];
    public Belt beltwdg;
    public final Map<Integer, String> polowners = new HashMap<Integer, String>();
    public Bufflist buffs;
    public CraftDBWnd craftwnd = null;
    public ActWindow craftlist, buildlist, actlist;
    public TimerPanel timers;
    public StudyWnd studywnd;
    public Observable menuObservable = new Observable(){
	@Override
	public void notifyObservers(Object arg) {
	    setChanged();
	    super.notifyObservers(arg);
	    clearChanged();
	}
    };

	public abstract class Belt extends Widget {
	public Belt(Coord sz) {
	    super(sz);
	}

	public void keyact(final int slot) {
	    if(map != null) {
		Coord mvc = map.rootxlate(ui.mc);
		if(mvc.isect(Coord.z, map.sz)) {
		    map.delay(map.new Hittest(mvc) {
			    protected void hit(Coord pc, Coord2d mc, MapView.ClickInfo inf) {
				Object[] args = {slot, 1, ui.modflags(), mc.floor(OCache.posres)};
				if(inf != null)
				    args = Utils.extend(args, MapView.gobclickargs(inf));
				GameUI.this.wdgmsg("belt", args);
			    }
			    
			    protected void nohit(Coord pc) {
				GameUI.this.wdgmsg("belt", slot, 1, ui.modflags());
			    }
			});
		}
	    }
	}
    }
    
    @RName("gameui")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    String chrid = (String)args[0];
	    int plid = (Integer)args[1];
	    String genus = "";
	    if(args.length > 2)
		genus = (String)args[2];
	    return(new GameUI(chrid, plid, genus));
	}
    }
    
    private final Coord minimapc;
    public GameUI(String chrid, long plid, String genus) {
	this.chrid = chrid;
	this.plid = plid;
	this.genus = genus;
	setcanfocus(true);
	setfocusctl(true);
	chat = add(new ChatUI(0, 0));
	if(Utils.getprefb("chatvis", true)) {
	    chat.hresize(chat.savedh);
	    chat.show();
	}
	beltwdg.raise();
	ulpanel = add(new Hidepanel("gui-ul", null, new Coord(-1, -1)));
	umpanel = add(new Hidepanel("gui-um", null, new Coord( 0, -1)));
	urpanel = add(new Hidepanel("gui-ur", null, new Coord( 1, -1)));
	blpanel = add(new Hidepanel("gui-bl", null, new Coord(-1,  1)));
	brpanel = add(new Hidepanel("gui-br", null, new Coord( 1,  1)) {
		public void move(double a) {
		    super.move(a);
		    menupanel.move();
		}
	    });
	menupanel = add(new Hidepanel("menu", new Indir<Coord>() {
		    public Coord get() {
			return(new Coord(GameUI.this.sz.x, Math.min(brpanel.c.y - 79, GameUI.this.sz.y - menupanel.sz.y)));
		    }
		}, new Coord(1, 0)));
	Tex lbtnbg = Resource.loadtex("gfx/hud/lbtn-bg");
	blpanel.add(new Img(Resource.loadtex("gfx/hud/blframe")), 0, lbtnbg.sz().y - 33);
	blpanel.add(new Img(lbtnbg), 0, 0);
	minimapc = new Coord(4, 34 + (lbtnbg.sz().y - 33));
	brpanel.add(new Img(Resource.loadtex("gfx/hud/brframe")), 0, 0);
	menupanel.add(new MainMenu(), 0, 0);
	mapbuttons();
	foldbuttons();
	portrait = ulpanel.add(new Avaview(Avaview.dasz, plid, "avacam") {
		public boolean mousedown(Coord c, int button) {
		    return(true);
		}
	    }, new Coord(10, 10));
	buffs = ulpanel.add(new Bufflist(), new Coord(95, 65));
	umpanel.add(calendar = new Cal(), new Coord(0, 10));
	eqproxy = ulpanel.add(new EquipProxy(new int[]{6, 7, 11, 5}), new Coord(420, 5));
	filter = add(new FilterWnd());
	syslog = chat.add(new ChatUI.Log("System"));
	opts = add(new OptWnd());
	opts.hide();
	zerg = add(new Zergwnd(), Utils.getprefc("wndc-zerg", new Coord(187, 50)));
	zerg.hide();
	placemmap();
    }

    private void mapbuttons() {
	blpanel.add(new IButton("gfx/hud/lbtn-claim", "", "-d", "-h") {
		{tooltip = Text.render("Display personal claims");}
		public void click() {
		    if((map != null) && !map.visol(0))
			map.enol(0, 1);
		    else
			map.disol(0, 1);
		}
	    }, 0, 0);
	blpanel.add(new IButton("gfx/hud/lbtn-vil", "", "-d", "-h") {
		{tooltip = Text.render("Display village claims");}
		public void click() {
		    if((map != null) && !map.visol(2))
			map.enol(2, 3);
		    else
			map.disol(2, 3);
		}
	    }, 0, 0);
	blpanel.add(new IButton("gfx/hud/lbtn-rlm", "", "-d", "-h") {
		{tooltip = Text.render("Display realms");}
		public void click() {
		    if((map != null) && !map.visol(4))
			map.enol(4, 5);
		    else
			map.disol(4, 5);
		}
	    }, 0, 0);
	blpanel.add(new MenuButton2("lbtn-map", "Map", TOGGLE_MAP, KeyEvent.VK_A, CTRL));
    }

    @Override
    protected void attach(UI ui) {
	ui.gui = this;
	super.attach(ui);
	timers = add(new TimerPanel(), 250, 100);
    }

    @Override
    public void destroy() {
	super.destroy();
	ui.gui = null;
    }

    /* Ice cream */
    private final IButton[] fold_br = new IButton[4];
    private final IButton[] fold_bl = new IButton[2];
    private void updfold(boolean reset) {
	int br;
	if(brpanel.tvis && menupanel.tvis)
	    br = 0;
	else if(brpanel.tvis && !menupanel.tvis)
	    br = 1;
	else if(!brpanel.tvis && !menupanel.tvis)
	    br = 2;
	else
	    br = 3;
	for(int i = 0; i < fold_br.length; i++)
	    fold_br[i].show(i == br);

	fold_bl[1].show(!blpanel.tvis);

	if(reset)
	    resetui();
    }

    private void foldbuttons() {
	final Tex rdnbg = Resource.loadtex("gfx/hud/rbtn-maindwn");
	final Tex rupbg = Resource.loadtex("gfx/hud/rbtn-upbg");
	fold_br[0] = new IButton("gfx/hud/rbtn-dwn", "", "-d", "-h") {
		public void draw(GOut g) {g.image(rdnbg, Coord.z); super.draw(g);}
		public void click() {
		    menupanel.cshow(false);
		    updfold(true);
		}
	    };
	fold_br[1] = new IButton("gfx/hud/rbtn-dwn", "", "-d", "-h") {
		public void draw(GOut g) {g.image(rdnbg, Coord.z); super.draw(g);}
		public void click() {
		    brpanel.cshow(false);
		    updfold(true);
		}
	    };
	fold_br[2] = new IButton("gfx/hud/rbtn-up", "", "-d", "-h") {
		public void draw(GOut g) {g.image(rupbg, Coord.z); super.draw(g);}
		public void click() {
		    menupanel.cshow(true);
		    updfold(true);
		}
		public void presize() {
		    this.c = parent.sz.sub(this.sz);
		}
	    };
	fold_br[3] = new IButton("gfx/hud/rbtn-dwn", "", "-d", "-h") {
		public void draw(GOut g) {g.image(rdnbg, Coord.z); super.draw(g);}
		public void click() {
		    brpanel.cshow(true);
		    updfold(true);
		}
	    };
	menupanel.add(fold_br[0], 0, 0);
	fold_br[0].lower();
	brpanel.adda(fold_br[1], brpanel.sz.x, 32, 1, 1);
	adda(fold_br[2], 1, 1);
	fold_br[2].lower();
	menupanel.add(fold_br[3], 0, 0);
	fold_br[3].lower();

	final Tex lupbg = Resource.loadtex("gfx/hud/lbtn-upbg");
	fold_bl[0] = new IButton("gfx/hud/lbtn-dwn", "", "-d", "-h") {
		public void click() {
		    blpanel.cshow(false);
		    updfold(true);
		}
	    };
	fold_bl[1] = new IButton("gfx/hud/lbtn-up", "", "-d", "-h") {
		public void draw(GOut g) {g.image(lupbg, Coord.z); super.draw(g);}
		public void click() {
		    blpanel.cshow(true);
		    updfold(true);
		}
		public void presize() {
		    this.c = new Coord(0, parent.sz.y - sz.y);
		}
	    };
	blpanel.add(fold_bl[0], 0, 0);
	adda(fold_bl[1], 0, 1);
	fold_bl[1].lower();

	updfold(false);
    }

    @Override
    public void bound() {
	BuffToggles.init(this);
    }

    protected void added() {
	resize(parent.sz);
	ui.cons.out = new java.io.PrintWriter(new java.io.Writer() {
		StringBuilder buf = new StringBuilder();
		
		public void write(char[] src, int off, int len) {
		    List<String> lines = new ArrayList<String>();
		    synchronized(this) {
			buf.append(src, off, len);
			int p;
			while((p = buf.indexOf("\n")) >= 0) {
			    lines.add(buf.substring(0, p));
			    buf.delete(0, p + 1);
			}
		    }
		    for(String ln : lines)
			syslog.append(ln, Color.WHITE);
		}
		
		public void close() {}
		public void flush() {}
	    });
	Debug.log = ui.cons.out;
	opts.c = sz.sub(opts.sz).div(2);
    }

    public void toggleCraftList() {
	if(craftlist == null){
	    craftlist = add(new ActWindow("Craft...", "paginae/craft/.+"));
	    craftlist.addtwdg(craftlist.add(new IButton("gfx/hud/btn-help", "","-d","-h"){
		@Override
		public void click() {
		    ItemFilter.showHelp(ui, HELP_SIMPLE, HELP_CURIO, HELP_FEP, HELP_ARMOR, HELP_SYMBEL, HELP_ATTR);
		}
	    }));
	} else if(craftlist.visible) {
	    craftlist.hide();
	} else {
	    craftlist.show();
	}
    }

    public void toggleBuildList() {
	if(buildlist == null){
	    buildlist = add(new ActWindow("Build...", "paginae/bld/.+"));
	} else if(buildlist.visible) {
	    buildlist.hide();
	} else {
	    buildlist.show();
	}
    }

    public void toggleActList() {
	if(actlist == null){
	    actlist = add(new ActWindow("Act...", "paginae/act/.+|paginae/pose/.+|paginae/gov/.+|paginae/add/.+|gfx/fx/msrad|ui/tt/q/quality"));
	} else if(actlist.visible) {
	    actlist.hide();
	} else {
	    actlist.show();
	}
    }
    
    public void toggleInventory() {
	if((invwnd != null) && invwnd.show(!invwnd.visible)) {
	    invwnd.raise();
	    fitwdg(invwnd);
	}
    }
    
    public void toggleEquipment() {
	if((equwnd != null) && equwnd.show(!equwnd.visible)) {
	    equwnd.raise();
	    fitwdg(equwnd);
	}
    }
    
    public void toggleCharacter() {
	if((chrwdg != null) && chrwdg.show(!chrwdg.visible)) {
	    chrwdg.raise();
	    fitwdg(chrwdg);
	}
    }
    
    public void toggleKinList() {
	if(zerg.show(!zerg.visible)) {
	    zerg.raise();
	    fitwdg(zerg);
	    setfocus(zerg);
	}
    }
    
    public void toggleOptions() {
	if(opts.show(!opts.visible)) {
	    opts.raise();
	    fitwdg(opts);
	    setfocus(opts);
	}
    }
    
    public void toggleChat() {
	if(chat.visible && !chat.hasfocus) {
	    setfocus(chat);
	} else {
	    if(chat.targeth == 0) {
		chat.sresize(chat.savedh);
		setfocus(chat);
	    } else {
		chat.sresize(0);
	    }
	}
	Utils.setprefb("chatvis", chat.targeth != 0);
    }

    public void toggleCraftDB() {
	if(craftwnd == null) {
	    craftwnd = add(new CraftDBWnd());
	} else {
	    craftwnd.close();
	}
    }
    
    public void toggleMap() {
	if((mapfile != null) && mapfile.show(!mapfile.visible)) {
	    mapfile.raise();
	    fitwdg(mapfile);
	    setfocus(mapfile);
	}
    }

    public class Hidepanel extends Widget {
	public final String id;
	public final Coord g;
	public final Indir<Coord> base;
	public boolean tvis;
	private double cur;

	public Hidepanel(String id, Indir<Coord> base, Coord g) {
	    this.id = id;
	    this.base = base;
	    this.g = g;
	    cur = show(tvis = Utils.getprefb(id + "-visible", true))?0:1;
	}

	public <T extends Widget> T add(T child) {
	    super.add(child);
	    pack();
	    if(parent != null)
		move();
	    return(child);
	}

	public Coord base() {
	    if(base != null) return(base.get());
	    return(new Coord((g.x > 0)?parent.sz.x:(g.x < 0)?0:(parent.sz.x / 2),
			     (g.y > 0)?parent.sz.y:(g.y < 0)?0:(parent.sz.y / 2)));
	}

	public void move(double a) {
	    cur = a;
	    Coord c = new Coord(base());
	    if(g.x < 0)
		c.x -= (int)(sz.x * a);
	    else if(g.x > 0)
		c.x -= (int)(sz.x * (1 - a));
	    if(g.y < 0)
		c.y -= (int)(sz.y * a);
	    else if(g.y > 0)
		c.y -= (int)(sz.y * (1 - a));
	    this.c = c;
	}

	public void move() {
	    move(cur);
	}

	public void presize() {
	    move();
	}

	public boolean mshow(final boolean vis) {
	    clearanims(Anim.class);
	    if(vis)
		show();
	    new NormAnim(0.25) {
		final double st = cur, f = vis?0:1;

		public void ntick(double a) {
		    if((a == 1.0) && !vis)
			hide();
		    move(st + (Utils.smoothstep(a) * (f - st)));
		}
	    };
	    tvis = vis;
	    updfold(false);
	    return(vis);
	}

	public boolean mshow() {
	    return(mshow(Utils.getprefb(id + "-visible", true)));
	}

	public boolean cshow(boolean vis) {
	    Utils.setprefb(id + "-visible", vis);
	    if(vis != tvis)
		mshow(vis);
	    return(vis);
	}

	public void cdestroy(Widget w) {
	    parent.cdestroy(w);
	}
    }

    static class Hidewnd extends Window {
	Hidewnd(Coord sz, String cap, boolean lg) {
	    super(sz, cap, lg);
	}

	Hidewnd(Coord sz, String cap) {
	    super(sz, cap);
	}

	public void wdgmsg(Widget sender, String msg, Object... args) {
	    if((sender == this) && msg.equals("close")) {
		this.hide();
		return;
	    }
	    super.wdgmsg(sender, msg, args);
	}
    }

    static class Zergwnd extends Hidewnd {
	Tabs tabs = new Tabs(Coord.z, Coord.z, this);
	final TButton kin, pol, pol2;

	class TButton extends IButton {
	    Tabs.Tab tab = null;
	    final Tex inv;

	    TButton(String nm, boolean g) {
		super(Resource.loadimg("gfx/hud/buttons/" + nm + "u"), Resource.loadimg("gfx/hud/buttons/" + nm + "d"));
		if(g)
		    inv = Resource.loadtex("gfx/hud/buttons/" + nm + "g");
		else
		    inv = null;
	    }

	    public void draw(GOut g) {
		if((tab == null) && (inv != null))
		    g.image(inv, Coord.z);
		else
		    super.draw(g);
	    }

	    public void click() {
		if(tab != null) {
		    tabs.showtab(tab);
		    repack();
		}
	    }
	}

	Zergwnd() {
	    super(Coord.z, "Kith & Kin", true);
	    kin = add(new TButton("kin", false));
	    kin.tooltip = Text.render("Kin");
	    pol = add(new TButton("pol", true));
	    pol2 = add(new TButton("rlm", true));
	}

	private void repack() {
	    tabs.indpack();
	    kin.c = new Coord(0, tabs.curtab.contentsz().y + 20);
	    pol.c = new Coord(kin.c.x + kin.sz.x + 10, kin.c.y);
	    pol2.c = new Coord(pol.c.x + pol.sz.x + 10, pol.c.y);
	    this.pack();
	}

	Tabs.Tab ntab(Widget ch, TButton btn) {
	    Tabs.Tab tab = add(tabs.new Tab() {
		    public void cresize(Widget ch) {
			repack();
		    }
		}, tabs.c);
	    tab.add(ch, Coord.z);
	    btn.tab = tab;
	    repack();
	    return(tab);
	}

	void dtab(TButton btn) {
	    btn.tab.destroy();
	    btn.tab = null;
	    repack();
	}

	void addpol(Polity p) {
	    /* This isn't very nice. :( */
	    TButton btn = p.cap.equals("Village")?pol:pol2;
	    ntab(p, btn);
	    btn.tooltip = Text.render(p.cap);
	}
    }

    static class DraggedItem {
	final GItem item;
	final Coord dc;

	DraggedItem(GItem item, Coord dc) {
	    this.item = item; this.dc = dc;
	}
    }

    private void updhand() {
	if((hand.isEmpty() && (vhand != null)) || ((vhand != null) && !hand.contains(vhand.item))) {
	    ui.destroy(vhand);
	    vhand = null;
	}
	if(!hand.isEmpty() && (vhand == null)) {
	    DraggedItem fi = hand.iterator().next();
	    vhand = add(new ItemDrag(fi.dc, fi.item));
	}
    }

    public void toggleHand(){
	if (hand.isEmpty()) {
	    hand.addAll(handSave);
	    handSave.clear();
	    updhand();
	} else {
	    handSave.addAll(hand);
	    hand.clear();
	    updhand();
	}
    }

    public void toggleStudy() {
	studywnd.show(!studywnd.visible);
    }

    public void takeScreenshot() {
	if(Config.screenurl != null) {
	    Screenshooter.take(this, Config.screenurl);
	}
    }

    public void addcmeter(Widget meter) {
	ulpanel.add(meter);
	cmeters.add(meter);
	updcmeters();
    }

    public <T extends Widget> void delcmeter(Class<T> cl) {
	Widget widget = null;
	for (Widget meter : cmeters) {
	    if (cl.isAssignableFrom(meter.getClass())) {
		widget = meter;
		break;
	    }
	}
	if (widget != null) {
	    cmeters.remove(widget);
	    widget.destroy();
	    updcmeters();
	}
    }

    private void updcmeters() {
	int i = meters.size();
	for (Widget meter : cmeters) {
	    int x = ( i % 3) * (IMeter.fsz.x + 5);
	    int y = (i / 3) * (IMeter.fsz.y + 2);
	    meter.c = new Coord(portrait.c.x + portrait.sz.x + 10 + x, portrait.c.y + y);
	    i++;
	}
    }

    private String mapfilename() {
	StringBuilder buf = new StringBuilder();
	buf.append(genus);
	String chrid = Utils.getpref("mapfile/" + this.chrid, "");
	if(!chrid.equals("")) {
	    if(buf.length() > 0) buf.append('/');
	    buf.append(chrid);
	}
	return(buf.toString());
    }

    public Coord optplacement(Widget child, Coord org) {
	Set<Window> closed = new HashSet<>();
	Set<Coord> open = new HashSet<>();
	open.add(org);
	Coord opt = null;
	double optscore = Double.NEGATIVE_INFINITY;
	Coord plc = null;
	{
	    Gob pl = map.player();
	    if(pl != null)
		plc = pl.sc;
	}
	Area parea = Area.sized(Coord.z, sz);
	while(!open.isEmpty()) {
	    Coord cur = Utils.take(open);
	    double score = 0;
	    Area tarea = Area.sized(cur, child.sz);
	    if(parea.isects(tarea)) {
		double outside = 1.0 - (((double)parea.overlap(tarea).area()) / ((double)tarea.area()));
		if((outside > 0.75) && !cur.equals(org))
		    continue;
		score -= Math.pow(outside, 2) * 100;
	    } else {
		if(!cur.equals(org))
		    continue;
		score -= 100;
	    }
	    {
		boolean any = false;
		for(Widget wdg = this.child; wdg != null; wdg = wdg.next) {
		    if(!(wdg instanceof Window))
			continue;
		    Window wnd = (Window)wdg;
		    if(!wnd.visible)
			continue;
		    Area warea = wnd.parentarea(this);
		    if(warea.isects(tarea)) {
			any = true;
			score -= ((double)warea.overlap(tarea).area()) / ((double)tarea.area());
			if(!closed.contains(wnd)) {
			    open.add(new Coord(wnd.c.x - child.sz.x, cur.y));
			    open.add(new Coord(cur.x, wnd.c.y - child.sz.y));
			    open.add(new Coord(wnd.c.x + wnd.sz.x, cur.y));
			    open.add(new Coord(cur.x, wnd.c.y + wnd.sz.y));
			    closed.add(wnd);
			}
		    }
		}
		if(!any)
		    score += 10;
	    }
	    if(plc != null) {
		if(tarea.contains(plc))
		    score -= 100;
		else
		    score -= (1 - Math.pow(tarea.closest(plc).dist(plc) / sz.dist(Coord.z), 2)) * 1.5;
	    }
	    score -= (cur.dist(org) / sz.dist(Coord.z)) * 0.75;
	    if(score > optscore) {
		optscore = score;
		opt = cur;
	    }
	}
	return(opt);
    }

    private void savewndpos() {
	if(invwnd != null)
	    Utils.setprefc("wndc-inv", invwnd.c);
	if(equwnd != null)
	    Utils.setprefc("wndc-equ", equwnd.c);
	if(chrwdg != null)
	    Utils.setprefc("wndc-chr", chrwdg.sz);
	if(zerg != null)
	    Utils.setprefc("wndc-zerg", zerg.c);
	if(mapfile != null) {
	    Utils.setprefc("wndc-map", mapfile.c);
	    Utils.setprefc("wndsz-map", mapfile.asz);
	}
    }

    public void addchild(Widget child, Object... args) {
	String place = ((String)args[0]).intern();
	if(place == "mapview") {
	    child.resize(sz);
	    map = add((MapView)child, Coord.z);
	    map.lower();
	    if(mmap != null)
		ui.destroy(mmap);
	    if(mapfile != null) {
		ui.destroy(mapfile);
		mapfile = null;
	    }
	    mmap = blpanel.add(new LocalMiniMap2(new Coord(133, 133), map), minimapc);
	    mmap.lower();
	    if(ResCache.global != null) {
		MapFile file = MapFile.load(ResCache.global, mapfilename());
		mmap.save(file);
		mapfile = new MapWnd2(mmap.save, map, Utils.getprefc("wndsz-map", new Coord(700, 500)), "Map");
		mapfile.hide();
		add(mapfile, Utils.getprefc("wndc-map", new Coord(50, 50)));
	    }
	    placemmap();
	} else if(place == "menu") {
	    menu = (MenuGrid)brpanel.add(child, 20, 34);
	} else if(place == "fight") {
	    fv = urpanel.add((Fightview)child, 0, 0);
	} else if(place == "fsess") {
	    add(child, Coord.z);
	} else if(place == "inv") {
	    invwnd = new Hidewnd(Coord.z, "Inventory") {
		    public void cresize(Widget ch) {
			pack();
		    }
		};
	    invwnd.add(maininv = (Inventory)child, Coord.z);
	    invwnd.pack();
	    invwnd.hide();
	    add(invwnd, Utils.getprefc("wndc-inv", new Coord(100, 100)));
	} else if(place == "equ") {
	    equwnd = new Hidewnd(Coord.z, "Equipment");
	    equipory = equwnd.add((Equipory) child, Coord.z);
	    equwnd.pack();
	    equwnd.hide();
	    add(equwnd, Utils.getprefc("wndc-equ", new Coord(400, 10)));
	} else if(place == "hand") {
	    GItem g = add((GItem)child);
	    Coord lc = (Coord)args[1];
	    hand.add(new DraggedItem(g, lc));
	    updhand();
	} else if(place == "chr") {
	    studywnd = add(new StudyWnd());
	    studywnd.hide();
	    chrwdg = add((CharWnd)child, Utils.getprefc("wndc-chr", new Coord(300, 50)));
	    chrwdg.hide();
	    if(CFG.HUNGER_METER.get()) {
		addcmeter(new HungerMeter(chrwdg.glut));
	    }
	    if(CFG.FEP_METER.get()) {
		addcmeter(new FEPMeter(chrwdg.feps));
	    }
	} else if(place == "craft") {
	    final Widget mkwdg = child;
	    if(craftwnd != null){
		craftwnd.setMakewindow(mkwdg);
	    } else {
		if(makewnd == null) {
		    makewnd = add(new CraftWindow(), new Coord(400, 200));
		}
		makewnd.add(child);
		makewnd.pack();
		makewnd.raise();
		makewnd.show();
	    }
	} else if(place == "buddy") {
	    zerg.ntab(buddies = (BuddyWnd)child, zerg.kin);
	} else if(place == "pol") {
	    Polity p = (Polity)child;
	    polities.add(p);
	    zerg.addpol(p);
	} else if(place == "chat") {
	    chat.addchild(child);
	} else if(place == "party") {
	    add(child, 10, 95);
	} else if(place == "meter") {
	    int x = (meters.size() % 3) * (IMeter.fsz.x + 5);
	    int y = (meters.size() / 3) * (IMeter.fsz.y + 2);
	    ulpanel.add(child, portrait.c.x + portrait.sz.x + 10 + x, portrait.c.y + y);
	    meters.add(child);
	    updcmeters();
	} else if(place == "buff") {
	    buffs.addchild(child);
	} else if(place == "qq") {
	    if(qqview != null)
		qqview.reqdestroy();
	    final Widget cref = qqview = child;
	    add(new AlignPanel() {
		    {add(cref);}

		    protected Coord getc() {
			return(new Coord(10, GameUI.this.sz.y - blpanel.sz.y - this.sz.y - 10));
		    }

		    public void cdestroy(Widget ch) {
			qqview = null;
			destroy();
		    }
		});
	} else if(place == "misc") {
	    Coord c;
	    if(args[1] instanceof Coord) {
		c = (Coord)args[1];
	    } else if(args[1] instanceof Coord2d) {
		c = ((Coord2d)args[1]).mul(new Coord2d(this.sz.sub(child.sz))).round();
		c = optplacement(child, c);
	    } else if(args[1] instanceof String) {
		c = relpos((String)args[1], child, (args.length > 2) ? ((Object[])args[2]) : new Object[] {}, 0);
	    } else {
		throw(new UI.UIException("Illegal gameui child", place, args));
	    }
	    add(child, c);
	} else if(place == "abt") {
	    add(child, Coord.z);
	} else {
	    throw(new UI.UIException("Illegal gameui child", place, args));
	}
    }

    public void cdestroy(Widget w) {
	if(w instanceof GItem) {
	    for(Iterator<DraggedItem> i = hand.iterator(); i.hasNext();) {
		DraggedItem di = i.next();
		if(di.item == w) {
		    i.remove();
		    updhand();
		}
	    }
	} else if(polities.contains(w)) {
	    polities.remove(w);
	    zerg.dtab(zerg.pol);
	} else if(w == chrwdg) {
	    chrwdg = null;
	}
	meters.remove(w);
	cmeters.remove(w);
	updcmeters();
    }
    
    public void placemmap() {
	if(mmap == null) {return;}
	if(mmap.parent != null) {
	    mmap.unlink();
	}
	mmap.sz = new Coord(133, 133);
	blpanel.add(mmap, minimapc);
	blpanel.show();
	mmap.lower();
    }

    private static final Resource.Anim progt = Resource.local().loadwait("gfx/hud/prog").layer(Resource.animc);
    private Tex curprog = null;
    private int curprogf, curprogb;
    private void drawprog(GOut g, double prog) {
	int fr = Utils.clip((int)Math.floor(prog * progt.f.length), 0, progt.f.length - 2);
	int bf = Utils.clip((int)(((prog * progt.f.length) - fr) * 255), 0, 255);
	if((curprog == null) || (curprogf != fr) || (curprogb != bf)) {
	    if(curprog != null)
		curprog.dispose();
	    WritableRaster buf = PUtils.imgraster(progt.f[fr][0].sz);
	    PUtils.blit(buf, progt.f[fr][0].img.getRaster(), Coord.z);
	    PUtils.blendblit(buf, progt.f[fr + 1][0].img.getRaster(), Coord.z, bf);
	    BufferedImage img = PUtils.rasterimg(buf);

	    BufferedImage txt = Text.renderstroked(String.format("%d%%", (int) (100 * prog))).img;
	    img.getGraphics().drawImage(txt, 24 - txt.getWidth() / 2, 9 - txt.getHeight() / 2, null);

	    curprog = new TexI(img); curprogf = fr; curprogb = bf;
	}
	g.aimage(curprog, new Coord(sz.x / 2, (sz.y * 4) / 10), 0.5, 0.5);
    }

    public void draw(GOut g) {
	beltwdg.c = new Coord(chat.c.x, Math.min(chat.c.y - beltwdg.sz.y + 4, sz.y - beltwdg.sz.y));
	super.draw(g);
	if(prog >= 0)
	    drawprog(g, prog);
	int by = sz.y;
	if(chat.visible)
	    by = Math.min(by, chat.c.y);
	if(beltwdg.visible)
	    by = Math.min(by, beltwdg.c.y);
	if(cmdline != null) {
	    drawcmd(g, new Coord(blpw + 10, by -= 20));
	} else if(lastmsg != null) {
	    if((Utils.rtime() - msgtime) > 3.0) {
		lastmsg = null;
	    } else {
		g.chcolor(0, 0, 0, 192);
		g.frect(new Coord(blpw + 8, by - 22), lastmsg.sz().add(4, 4));
		g.chcolor();
		g.image(lastmsg.tex(), new Coord(blpw + 10, by -= 20));
	    }
	}
	if(!chat.visible) {
	    chat.drawsmall(g, new Coord(blpw + 10, by), 50);
	}
    }
    
    private double lastwndsave = 0;
    public void tick(double dt) {
	super.tick(dt);
	double now = Utils.rtime();
	if(now - lastwndsave > 60) {
	    savewndpos();
	    lastwndsave = now;
	}
	double idle = now - ui.lastevent;
	if(!afk && (idle > 300)) {
	    afk = true;
	    wdgmsg("afk");
	} else if(afk && (idle <= 300)) {
	    afk = false;
	}
    }
    
    public void uimsg(String msg, Object... args) {
	if(msg == "err") {
	    String err = (String)args[0];
	    Reactor.EMSG.onNext(err);
	    error(err);
	} else if(msg == "msg") {
	    String text = (String)args[0];
	    Reactor.IMSG.onNext(text);
	    msg(text);
	} else if(msg == "prog") {
	    if(args.length > 0)
		prog = ((Number)args[0]).doubleValue() / 100.0;
	    else
		prog = -1;
	} else if(msg == "setbelt") {
	    int slot = (Integer)args[0];
	    if(args.length < 2) {
		belt[slot] = null;
	    } else {
		belt[slot] = ui.sess.getres((Integer)args[1]);
	    }
	} else if(msg == "polowner") {
	    int id = (Integer)args[0];
	    String o = (String)args[1];
	    boolean n = ((Integer)args[2]) != 0;
	    if(o != null)
		o = o.intern();
	    String cur = polowners.get(id);
	    if(map != null) {
		if((o != null) && (cur == null)) {
		    map.setpoltext(id, "Entering " + o);
		} else if((o == null) && (cur != null)) {
		    map.setpoltext(id, "Leaving " + cur);
		}
	    }
	    polowners.put(id, o);
	} else if(msg == "showhelp") {
	    Indir<Resource> res = ui.sess.getres((Integer)args[0]);
	    if(help == null)
		help = adda(new HelpWnd(res), 0.5, 0.5);
	    else
		help.res = res;
	} else if(msg == "map-mark") {
	    long gobid = ((Integer)args[0]) & 0xffffffff;
	    long oid = (Long)args[1];
	    Indir<Resource> res = ui.sess.getres((Integer)args[2]);
	    String nm = (String)args[3];
	    if(mapfile != null)
		mapfile.markobj(gobid, oid, res, nm);
	} else {
	    super.uimsg(msg, args);
	}
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if((sender == chrwdg) && (msg == "close")) {
	    chrwdg.hide();
	    return;
	} else if((sender == mapfile) && (msg == "close")) {
	    mapfile.hide();
	    return;
	} else if((sender == help) && (msg == "close")) {
	    ui.destroy(help);
	    help = null;
	    return;
	}
	super.wdgmsg(sender, msg, args);
    }

    private void fitwdg(Widget wdg) {
	if(wdg.c.x < 0)
	    wdg.c.x = 0;
	if(wdg.c.y < 0)
	    wdg.c.y = 0;
	if(wdg.c.x + wdg.sz.x > sz.x)
	    wdg.c.x = sz.x - wdg.sz.x;
	if(wdg.c.y + wdg.sz.y > sz.y)
	    wdg.c.y = sz.y - wdg.sz.y;
    }

    public static class MenuButton extends IButton {
	private final int gkey;

	MenuButton(String base, int gkey, String tooltip) {
	    super("gfx/hud/" + base, "", "-d", "-h");
	    this.gkey = (char)gkey;
	    this.tooltip = RichText.render(tooltip, 0);
	}
 
	@Override
	public Object tooltip(Coord c, Widget prev) {
	    return super.tooltip(c, prev);
	}
 
	public void click() {}

	public boolean globtype(char key, KeyEvent ev) {
	    if((gkey != -1) && (key == gkey)) {
		click();
		return(true);
	    }
	    return(super.globtype(key, ev));
	}
    }
    
    public static class MenuButton2 extends IButton {
	private final Action action;
	private final String tip;
 
	MenuButton2(String base, String tooltip, Action action, int code, int mods) {
	    super("gfx/hud/" + base, "", "-d", "-h");
	    this.action = action;
	    this.tip = tooltip;
	    KeyBinder.add(code, mods, action);
	}
 
	@Override
	public Object tooltip(Coord c, Widget prev) {
	    if(!checkhit(c)) {
		return null;
	    }
	    KeyBinder.KeyBind bind = KeyBinder.get(action);
	    String tt = tip;
	    if(bind != null && !bind.isEmpty()) {
		tt = String.format("%s ($col[255,255,0]{%s})", tip, bind.shortcut());
	    }
	    return RichText.render(tt, 0);
	}
 
	@Override
	public void click() {
	    action.run(ui.gui);
	}
    }

    private static final Tex menubg = Resource.loadtex("gfx/hud/rbtn-bg");
    public class MainMenu extends Widget {
	public MainMenu() {
	    super(menubg.sz());
	    add(new MenuButton2("rbtn-inv", "Inventory", TOGGLE_INVENTORY, KeyEvent.VK_TAB, NONE) , 0, 0);
	    add(new MenuButton2("rbtn-equ", "Equipment", TOGGLE_EQUIPMENT, KeyEvent.VK_E, CTRL), 0, 0);
	    add(new MenuButton2("rbtn-chr", "Character Sheet", TOGGLE_CHARACTER, KeyEvent.VK_T, CTRL), 0, 0);
	    add(new MenuButton2("rbtn-bud", "Kith & Kin", TOGGLE_KIN_LIST, KeyEvent.VK_B, CTRL), 0, 0);
	    add(new MenuButton2("rbtn-opt", "Options", TOGGLE_OPTIONS, KeyEvent.VK_O, CTRL), 0, 0);
	}

	public void draw(GOut g) {
	    g.image(menubg, Coord.z);
	    super.draw(g);
	}
    }

    public boolean globtype(char key, KeyEvent ev) {
	if(key == ':') {
	    entercmd();
	    return(true);
	} else if(key == ' ') {
	    toggleui();
	    return(true);
	} else if((key == 27) && (map != null) && !map.hasfocus) {
	    setfocus(map);
	    return(true);
	}
	return(super.globtype(key, ev));
    }
    
    public boolean mousedown(Coord c, int button) {
	return(super.mousedown(c, button));
    }

    private int uimode = 1;
    public void toggleui(int mode) {
	Hidepanel[] panels = {blpanel, brpanel, ulpanel, umpanel, urpanel, menupanel};
	switch(uimode = mode) {
	case 0:
	    for(Hidepanel p : panels)
		p.mshow(true);
	    break;
	case 1:
	    for(Hidepanel p : panels)
		p.mshow();
	    break;
	case 2:
	    for(Hidepanel p : panels)
		p.mshow(false);
	    break;
	}
    }

    public void resetui() {
	Hidepanel[] panels = {blpanel, brpanel, ulpanel, umpanel, urpanel, menupanel};
	for(Hidepanel p : panels)
	    p.cshow(p.tvis);
	uimode = 1;
    }

    public void toggleui() {
	toggleui((uimode + 1) % 3);
    }

    public void resize(Coord sz) {
	this.sz = sz;
	chat.resize(sz.x - blpw - brpw);
	chat.move(new Coord(blpw, sz.y));
	if(map != null)
	    map.resize(sz);
	beltwdg.c = new Coord(blpw + 10, sz.y - beltwdg.sz.y - 5);
	super.resize(sz);
    }
    
    public void presize() {
	resize(parent.sz);
    }
    
    public void msg(String msg, Color color, Color logcol) {
	msgtime = Utils.rtime();
	lastmsg = msgfoundry.render(msg, color);
	syslog.append(msg, logcol);
    }

    public void msg(String msg, Color color) {
	msg(msg, color, color);
	Audio.play(msgsfx);
    }

    private static final Resource errsfx = Resource.local().loadwait("sfx/error");
    public void error(String msg) {
	msg(msg, MsgType.ERROR);
    }

    private static final Resource msgsfx = Resource.local().loadwait("sfx/msg");
    private double lastmsgsfx = 0;
    public void msg(String msg) {
	msg(msg, MsgType.INFO);
	double now = Utils.rtime();
	if(now - lastmsgsfx > 0.1) {
	    Audio.play(msgsfx);
	    lastmsgsfx = now;
	}
    }
    
    public void msg(String msg, MsgType type) {
	msg(msg, type.color, type.logcol);
	if(type.sfx != null) {
	    Audio.play(type.sfx);
	}
    }

    public enum MsgType {
	INFO(Color.WHITE), GOOD(Color.GREEN), BAD(Color.RED),
	ERROR(new Color(192, 0, 0), new Color(255, 0, 0), "sfx/error");

	public final Color color, logcol;
	public final Resource sfx;

	MsgType(Color color) {
	    this(color, color, null);
	}

	MsgType(Color color, Color logcol, String sfx) {
	    this.logcol = logcol;
	    this.color = color;
	    this.sfx = (sfx != null) ? Resource.local().loadwait(sfx) : null;
	}
    }

    public void act(String... args) {
	wdgmsg("act", (Object[])args);
    }

    public void act(int mods, Coord mc, Gob gob, String... args) {
	int n = args.length;
	Object[] al = new Object[n];
	System.arraycopy(args, 0, al, 0, n);
	if(mc != null) {
	    al = Utils.extend(al, al.length + 2);
	    al[n++] = mods;
	    al[n++] = mc;
	    if(gob != null) {
		al = Utils.extend(al, al.length + 2);
		al[n++] = (int)gob.id;
		al[n++] = gob.rc;
	    }
	}
	wdgmsg("act", al);
    }

    public class FKeyBelt extends Belt implements DTarget, DropTarget {
	public final int beltkeys[] = {KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4,
				       KeyEvent.VK_F5, KeyEvent.VK_F6, KeyEvent.VK_F7, KeyEvent.VK_F8,
				       KeyEvent.VK_F9, KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12};
	public int curbelt = 0;

	public FKeyBelt() {
	    super(new Coord(450, 34));
	}

	private Coord beltc(int i) {
	    return(new Coord(((invsq.sz().x + 2) * i) + (10 * (i / 4)), 0));
	}
    
	private int beltslot(Coord c) {
	    for(int i = 0; i < 12; i++) {
		if(c.isect(beltc(i), invsq.sz()))
		    return(i + (curbelt * 12));
	    }
	    return(-1);
	}
    
	public void draw(GOut g) {
	    for(int i = 0; i < 12; i++) {
		int slot = i + (curbelt * 12);
		Coord c = beltc(i);
		g.image(invsq, beltc(i));
		try {
		    if(belt[slot] != null) {
			Resource.Image img = belt[slot].get().layer(Resource.imgc);
			if(img == null)
			    throw(new NullPointerException("No image in " + belt[slot].get().name));
			g.image(img.tex(), c.add(1, 1));
		    }
		} catch(Loading e) {}
		g.chcolor(156, 180, 158, 255);
		FastText.aprintf(g, c.add(invsq.sz().sub(2, 0)), 1, 1, "F%d", i + 1);
		g.chcolor();
	    }
	}
	
	public boolean mousedown(Coord c, int button) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		if(button == 1)
		    GameUI.this.wdgmsg("belt", slot, 1, ui.modflags());
		if(button == 3)
		    GameUI.this.wdgmsg("setbelt", slot, 1);
		return(true);
	    }
	    return(false);
	}

	public boolean globtype(char key, KeyEvent ev) {
	    if(key != 0 || ui.modctrl)
		return(false);
	    boolean M = (ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0;
	    for(int i = 0; i < beltkeys.length; i++) {
		if(ev.getKeyCode() == beltkeys[i]) {
		    if(M) {
			curbelt = i;
			return(true);
		    } else {
			keyact(i + (curbelt * 12));
			return(true);
		    }
		}
	    }
	    return(false);
	}
	
	public boolean drop(Coord c, Coord ul) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		GameUI.this.wdgmsg("setbelt", slot, 0);
		return(true);
	    }
	    return(false);
	}

	public boolean iteminteract(Coord c, Coord ul) {return(false);}
	
	public boolean dropthing(Coord c, Object thing) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		if(thing instanceof Resource) {
		    Resource res = (Resource)thing;
		    if(res.layer(Resource.action) != null) {
			GameUI.this.wdgmsg("setbelt", slot, res.name);
			return(true);
		    }
		}
	    }
	    return(false);
	}
    }
    
    private static final Tex nkeybg = Resource.loadtex("gfx/hud/hb-main");
    public class NKeyBelt extends Belt implements DTarget, DropTarget {
	public int curbelt = 0;
	final Coord pagoff = new Coord(5, 25);

	public NKeyBelt() {
	    super(nkeybg.sz());
	    adda(new IButton("gfx/hud/hb-btn-chat", "", "-d", "-h") {
		    Tex glow;
		    {
			this.tooltip = RichText.render("Chat ($col[255,255,0]{Ctrl+C})", 0);
			glow = new TexI(PUtils.rasterimg(PUtils.blurmask(up.getRaster(), 2, 2, Color.WHITE)));
			KeyBinder.add(KeyEvent.VK_C, CTRL, TOGGLE_CHAT);
		    }

		    public void click() {
			toggleChat();
		    }
	    
		@Override
		public Object tooltip(Coord c, Widget prev) {
		    if(!checkhit(c)) {
			return null;
		    }
		    KeyBinder.KeyBind bind = KeyBinder.get(TOGGLE_CHAT);
		    String tt = "Chat";
		    if(bind != null && !bind.isEmpty()) {
			tt = String.format("%s ($col[255,255,0]{%s})", tt, bind.shortcut());
		    }
		    return RichText.render(tt, 0);
		}
	 
		public void draw(GOut g) {
			super.draw(g);
			Color urg = chat.urgcols[chat.urgency];
			if(urg != null) {
			    GOut g2 = g.reclipl(new Coord(-2, -2), g.sz.add(4, 4));
			    g2.chcolor(urg.getRed(), urg.getGreen(), urg.getBlue(), 128);
			    g2.image(glow, Coord.z);
			}
		    }
		}, sz, 1, 1);
	}
	
	private Coord beltc(int i) {
	    return(pagoff.add(((invsq.sz().x + 2) * i) + (10 * (i / 5)), 0));
	}
    
	private int beltslot(Coord c) {
	    for(int i = 0; i < 10; i++) {
		if(c.isect(beltc(i), invsq.sz()))
		    return(i + (curbelt * 12));
	    }
	    return(-1);
	}
    
	public void draw(GOut g) {
	    g.image(nkeybg, Coord.z);
	    for(int i = 0; i < 10; i++) {
		int slot = i + (curbelt * 12);
		Coord c = beltc(i);
		g.image(invsq, beltc(i));
		try {
		    if(belt[slot] != null) {
			Resource.Image img = belt[slot].get().layer(Resource.imgc);
			if(img == null)
			    throw(new NullPointerException("No image in " + belt[slot].get().name));
			g.image(img.tex(), c.add(1, 1));
		    }
		} catch(Loading e) {}
		g.chcolor(156, 180, 158, 255);
		FastText.aprintf(g, c.add(invsq.sz().sub(2, 0)), 1, 1, "%d", (i + 1) % 10);
		g.chcolor();
	    }
	    super.draw(g);
	}
	
	public boolean mousedown(Coord c, int button) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		if(button == 1)
		    GameUI.this.wdgmsg("belt", slot, 1, ui.modflags());
		if(button == 3)
		    GameUI.this.wdgmsg("setbelt", slot, 1);
		return(true);
	    }
	    return(super.mousedown(c, button));
	}

	public boolean globtype(char key, KeyEvent ev) {
	    if(key != 0 || ui.modctrl)
		return(false);
	    int c = ev.getKeyCode();
	    if((c < KeyEvent.VK_0) || (c > KeyEvent.VK_9))
		return(false);
	    int i = Utils.floormod(c - KeyEvent.VK_0 - 1, 10);
	    boolean M = (ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0;
	    if(M) {
		curbelt = i;
	    } else {
		keyact(i + (curbelt * 12));
	    }
	    return(true);
	}
	
	public boolean drop(Coord c, Coord ul) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		GameUI.this.wdgmsg("setbelt", slot, 0);
		return(true);
	    }
	    return(false);
	}

	public boolean iteminteract(Coord c, Coord ul) {return(false);}
	
	public boolean dropthing(Coord c, Object thing) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		if(thing instanceof Resource) {
		    Resource res = (Resource)thing;
		    if(res.layer(Resource.action) != null) {
			GameUI.this.wdgmsg("setbelt", slot, res.name);
			return(true);
		    }
		}
	    }
	    return(false);
	}
    }
    
    {
	String val = Utils.getpref("belttype", "n");
	if(val.equals("n")) {
	    beltwdg = add(new NKeyBelt());
	} else if(val.equals("f")) {
	    beltwdg = add(new FKeyBelt());
	} else {
	    beltwdg = add(new NKeyBelt());
	}
    }
    
    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
    {
	cmdmap.put("afk", new Console.Command() {
		public void run(Console cons, String[] args) {
		    afk = true;
		    wdgmsg("afk");
		}
	    });
	cmdmap.put("act", new Console.Command() {
		public void run(Console cons, String[] args) {
		    Object[] ad = new Object[args.length - 1];
		    System.arraycopy(args, 1, ad, 0, ad.length);
		    wdgmsg("act", ad);
		}
	    });
	cmdmap.put("belt", new Console.Command() {
		public void run(Console cons, String[] args) {
		    if(args[1].equals("f")) {
			beltwdg.destroy();
			beltwdg = add(new FKeyBelt());
			Utils.setpref("belttype", "f");
			resize(sz);
		    } else if(args[1].equals("n")) {
			beltwdg.destroy();
			beltwdg = add(new NKeyBelt());
			Utils.setpref("belttype", "n");
			resize(sz);
		    }
		}
	    });
	cmdmap.put("chrmap", new Console.Command() {
		public void run(Console cons, String[] args) {
		    Utils.setpref("mapfile/" + chrid, args[1]);
		}
	    });
	cmdmap.put("tool", new Console.Command() {
		public void run(Console cons, String[] args) {
		    add(gettype(args[1]).create(ui, new Object[0]), 200, 200);
		}
	    });
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }
}
