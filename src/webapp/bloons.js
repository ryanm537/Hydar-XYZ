
function bloons($_p) {
  var spec = null;
  var mapid = 1;
  var tier = 0;
  var frames = 0;
  var lowGraphics = false;
  var lost = 2;
  var mon = [];
  var dart = [];
  var spikes = [];
  var bal = [];
  var pops = [];
  var expl = [];
  var m = 0;
  var place = 0, bigbloons = true;
  var monkey_is_pressed = 0;
  var monkey_on = 0;
  var mon_on_menu = 650;
  var ee1 = false;
  var menu_hight = -2;
  var money = 650;
  var lives = 100;
  var press = false;
  var prices = {
    dartm: 150,
    dartm1: 480,
    superm: 1000,
    superm1: 2950,
    dartlingm: 745,
    bananafarm: 300,
    ninja: 500,
    engineer: 450,
    sentryengineer: 820,
    lc: 5200,
    spikeopult: 1230,
    roadspikes: 75,
    bomb: 800
  };
  var up = {
    dartm: 220,
    dartm1: 0,
    superm: 2500,
    superm1: 0,
    dartlingm: 4200,
    bananafarm: 5000,
    ninja: 400,
    ninja1: 0,
    engineer: 450,
    sentryengineer: 0,
    lc: 0,
    spikeopult: 2400,
    triplepult: 0,
    roadspikes: 0
  };
  var up = [
    0,
    220,
    2500,
    12000,
    0,
    0,
    0,
    4200,
    400,
    450,
    2400,
    0,
    0,
    0,
    1000,
    0,
    0,
    0
  ];
  var name = [
    'error',
    'Dart Monkey\nLevel 1',
    'Super Monkey\nLevel 1',
    'Banana Farm\nLevel 1',
    '',
    'Dart Monkey\nLevel 2',
    'Super Monkey\nLevel 2',
    'Dartling Gun\nLevel 1',
    'Ninja Monkey\nLevel 1',
    'Monkey Engineer\nLevel 1',
    'Spike-o-pult\nLevel 1',
    'Laser Cannon\nLevel 2',
    'BRF\nLevel 2',
    'Ninja Monkey\nLevel 2',
    'Bomb Tower\nLevel 1',
    'Bomb Tower\nLevel 2',
    'Monkey Engineer\nLevel 2',
    'Sentry'
  ];
  var uname = [
    'error',
    'Sharp Shots\nIncreases pierce and range.',
    'Twin Darts\nFires 2 darts with increased range.',
    'BRF\n Generates 50x the cash.',
    'MAX UPGRADES\n',
    'MAX UPGRADES\n',
    'MAX UPGRADES\n',
    'Laser Cannon\nLasers are much more powerful\n than darts.',
    'Ninja Discipline\nIncreases pierce and range.',
    'Sentry Gun\nBuilds bloon-popping sentry turrets.',
    'Triple Shot\nFires 3 spiked balls at a time.',
    'MAX UPGRADES\n',
    'MAX UPGRADES\n',
    'MAX UPGRADES\n',
    'Heavy Munitions\nIncreases radius and DMG.',
    'MAX UPGRADES\n',
    'MAX UPGRADES\n',
    'MAX UPGRADES\n'
  ];
  var mouseTicks = 0;
  var bloonPop = 0;
  var sp = [
    -1,
    135,
    850,
    220,
    0,
    300,
    2300,
    650,
    410,
    340,
    1000,
    4600,
    9850,
    700,
    635,
    1380,
    700,
    0
  ];
  var lowGraphics = false;
  var code = 0;
  var i = 0;
  var i = 0;
  var x1 = 0;
  var i = 0;
  var ww = 0;
  var d = 15;
  var d2 = 30;
  var x = -100;
  var y = -100;
  var i = 0;
  var in_range = [];
  var www = 0;
  var temp = monkey_on;
  var abc = false;
  var abc = true;
  var i = 0;
  var abc = true;
  var i = 0;
  var abc = true;
  var i = 0;
  var abc = true;
  var i = 0;
  var abc = true;
  var i = 0;
  var abc = true;
  var i = 0;
  var abc = true;
  var i = 0;
  var abc = true;
  var i = 0;
  var abc = true;
  var i = 0;
  var abc = true;
  var i = 0;
  var abc = true;
  var i = 0;
  var abc = true;
  var i = 0;
  var i = 0;
  var m2 = 0;
  
  $_p.setup = function () {
    $_p.frameRate(30);
    $_p.angleMode($_p.DEGREES);
    $_p.createCanvas(400, 400).style('display','block');
  };
  
  function sell(i) {
    if (i === -1) {
      return
    }
    money += sp[mon[i].t];
    mon[i].t = -999;
    mon.splice(i, 1)
  }
  
  function dartlingGU(x1, y1) {
    $_p.translate(x1, y1);
    $_p.fill(216, 214, 217);
    $_p.rect(206, 187, 94, 25);
    $_p.ellipse(200, 200, 37, 37);
    $_p.line(218, 205, 300, 205);
    $_p.line(218, 195, 300, 195);
    $_p.rect(226, 184, 8, 3);
    $_p.rect(226, 212, 8, 3);
    $_p.fill(6, 204, 20);
    $_p.rect(216, 187, 28, 25);
    $_p.rect(289, 187, 7, 25);
    $_p.rect(263, 187, 7, 25);
    $_p.rect(224, 177, 12, 7);
    $_p.rect(224, 215, 12, 7);
    $_p.fill(255, 230, 0);
    $_p.rect(236, 187, 4, 25);
    $_p.rect(227, 187, 4, 25)
  }
  
  function shadowGun(x1, y1, ro) {
    $_p.push();
    $_p.translate(x1, y1);
    $_p.rotate(ro);
    $_p.translate(-200, -200);
    $_p.fill(115, 109, 109);
    $_p.rect(206, 187, 94, 25);
    $_p.ellipse(200, 200, 37, 37);
    $_p.line(218, 205, 300, 205);
    $_p.line(218, 195, 300, 195);
    $_p.rect(226, 184, 8, 3);
    $_p.rect(226, 212, 8, 3);
    $_p.fill(255, 0, 0);
    $_p.rect(216, 187, 28, 25);
    $_p.rect(289, 187, 7, 25);
    $_p.rect(263, 187, 7, 25);
    $_p.rect(224, 177, 12, 7);
    $_p.rect(224, 215, 12, 7);
    $_p.fill(22, 36, 36);
    $_p.rect(236, 187, 4, 25);
    $_p.rect(227, 187, 4, 25);
    $_p.pop()
  }
  
  function rodM(x, y, r, ro) {
    $_p.strokeWeight(1);
    $_p.stroke(0, 0, 0);
    $_p.push();
    shadowGun(x, y, ro);
    $_p.translate(x, y);
    $_p.rotate(ro + 90);
    $_p.translate(-x, -y);
    $_p.noStroke();
    if (r) {
      $_p.fill(26, 22, 26, 60);
      $_p.ellipse(x, y, 100, 100)
    }
    if (!lowGraphics) {
      $_p.noFill();
      $_p.stroke(3, 2, 3);
      $_p.strokeWeight(4);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20);
      $_p.stroke(168, 54, 5);
      $_p.strokeWeight(2);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20)
    }
    if (!lowGraphics) {
      $_p.strokeWeight(1);
      $_p.stroke(0, 0, 0);
      $_p.fill(168, 54, 5);
      $_p.ellipse(x - 12, y + 2, 5, 7);
      $_p.ellipse(x + 12, y + 2, 5, 7);
      $_p.noStroke();
      $_p.fill(232, 138, 30);
      $_p.ellipse(x - 11.5, y + 1, 4, 4);
      $_p.ellipse(x + 11.5, y + 1, 4, 4)
    }
    $_p.fill(34, 199, 20);
    $_p.stroke(0, 0, 0);
    $_p.ellipse(x + 10, y - 5, 4, 10);
    $_p.stroke(20, 18, 18);
    $_p.fill(224, 81, 20);
    $_p.arc(x, y + 1, 23, 23, -1, 182);
    $_p.rect(x - 12, y - 3, 23, 4);
    $_p.noStroke();
    $_p.rect(x - 11, y - 3, 22, 6);
    $_p.fill(5, 5, 4);
    $_p.rect(x - 11, y - 3, 19.5, 6);
    $_p.arc(x - 1, y + 1, 20, 21, -1, 182);
    $_p.stroke(0, 0, 0);
    $_p.fill(125, 96, 66);
    $_p.arc(x, y - 2, 23, 12, 180, 360);
    $_p.stroke(0, 0, 0);
    $_p.fill(43, 37, 34);
    $_p.arc(x, y - 1, 20, 18, 210, 330);
    $_p.noStroke();
    $_p.fill(18, 17, 20);
    if (!lowGraphics) {
      $_p.ellipse(x, y, 10, 10)
    }
    $_p.fill(247, 8, 8);
    $_p.ellipse(x + 6, y - 5.5, 2.3, 2.3);
    $_p.ellipse(x - 6, y - 5.5, 2.3, 2.3);
    $_p.fill(42, 40, 48);
    $_p.pop()
  }
  
  function DGmonkey(x, y, r) {
    $_p.noStroke();
    if (r) {
      $_p.fill(0, 0, 0, 60);
      $_p.ellipse(x, y, 100, 100)
    }
    if (!lowGraphics) {
      $_p.noFill();
      $_p.stroke(0, 0, 0);
      $_p.strokeWeight(4);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20);
      $_p.stroke(168, 54, 5);
      $_p.strokeWeight(2);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20)
    }
    if (!lowGraphics) {
      $_p.strokeWeight(1);
      $_p.stroke(0, 0, 0);
      $_p.fill(168, 54, 5);
      $_p.ellipse(x - 12, y + 2, 5, 7);
      $_p.ellipse(x + 12, y + 2, 5, 7);
      $_p.noStroke();
      $_p.fill(232, 138, 30);
      $_p.ellipse(x - 11.5, y + 1, 4, 4);
      $_p.ellipse(x + 11.5, y + 1, 4, 4)
    }
    $_p.fill(199, 115, 18);
    $_p.stroke(0, 0, 0);
    $_p.ellipse(x + 10, y - 5, 4, 10);
    $_p.stroke(51, 19, 0);
    $_p.fill(105, 34, 3);
    $_p.arc(x, y + 1, 23, 23, -1, 182);
    $_p.rect(x - 12, y - 3, 23, 4);
    $_p.noStroke();
    $_p.rect(x - 11, y - 3, 22, 6);
    $_p.fill(168, 54, 5);
    $_p.rect(x - 11, y - 3, 19.5, 6);
    $_p.arc(x - 1, y + 1, 20, 21, -1, 182);
    $_p.stroke(0, 0, 0);
    $_p.fill(204, 102, 0);
    $_p.arc(x, y - 2, 23, 12, 180, 360);
    $_p.stroke(0, 0, 0);
    $_p.fill(232, 131, 54);
    $_p.arc(x, y - 1, 20, 18, 210, 330);
    $_p.noStroke();
    $_p.fill(168, 54, 5);
    if (!lowGraphics) {
      $_p.ellipse(x, y, 10, 10)
    }
    $_p.fill(0, 0, 0);
    $_p.ellipse(x + 6, y - 5.5, 2.3, 2.3);
    $_p.ellipse(x - 6, y - 5.5, 2.3, 2.3);
    $_p.fill(7, 168, 17);
    $_p.ellipse(192, -204, 29, 22)
  }
  
  function dartlingGun(x2, y2, r2) {
    $_p.push();
    $_p.translate(x2 + 150, y2 - 150);
    $_p.rotate(90);
    $_p.scale(0.75, 0.75);
    $_p.stroke(0, 0, 0);
    dartlingGU();
    $_p.push();
    $_p.rotate(92);
    DGmonkey(192, -210, r2);
    $_p.pop();
    $_p.pop()
  }
  
  $_p.keyPressed = function () {
    code = $_p.keyCode
  };
  
  $_p.keyReleased = function () {
    code = 0
  };
  
  function aimMouse(x, y) {
    if (x > $_p.mouseX && y > $_p.mouseY) {
      return -1 * $_p.atan((x - $_p.mouseX) / (y - $_p.mouseY)) + 90
    } else if (x < $_p.mouseX && y > $_p.mouseY) {
      return $_p.atan(($_p.mouseX - x) / (y - $_p.mouseY)) + 90
    } else if (x > $_p.mouseX && y < $_p.mouseY) {
      return $_p.atan((x - $_p.mouseX) / ($_p.mouseY - y)) + 270
    } else if (x < $_p.mouseX && y < $_p.mouseY) {
      return -1 * $_p.atan(($_p.mouseX - x) / ($_p.mouseY - y)) + 270
    }
  }
  
  function spikePult(x, y, r, ro) {
    $_p.noStroke();
    if (r) {
      $_p.fill(0, 0, 0, 60);
      $_p.ellipse(x, y, 360, 360)
    }
    $_p.noFill();
    $_p.stroke(135, 13, 13);
    $_p.strokeWeight(7);
    $_p.rect(x - 20, y - 50, 40, 60);
    $_p.stroke(0, 0, 0);
    $_p.rect(x - 2, y - 50, 2, 50);
    {
      $_p.fill(15, 15, 15);
      $_p.strokeWeight(4);
      $_p.line(x - 16, y + 10, x + 16, y + 10);
      $_p.line(x, y - 24, x, y + 24);
      $_p.strokeWeight(7);
      $_p.triangle(x - 6, y + 2, x - 6, y + 8, x - 10, y + 2);
      $_p.triangle(x - 6, y + 18, x - 6, y + 12, x - 10, y + 18);
      $_p.triangle(x - 6, y + 2, x - 6, y + 8, x + 5.2, y + 2);
      $_p.triangle(x + 8, y + 18, x + 8, y + 12, x + 12, y + 18);
      $_p.ellipse(x, y + 10, 20, 20)
    }
    $_p.strokeWeight(2);
    $_p.rect(x - 20, y - 45, 1, 50);
    $_p.rect(x + 20, y - 45, 1, 50);
    $_p.push();
    $_p.translate(x * 2, y * 2 - 50);
    $_p.rotate(180);
    if (!lowGraphics) {
      $_p.noFill();
      $_p.stroke(0, 0, 0);
      $_p.strokeWeight(4);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20);
      $_p.stroke(168, 54, 5);
      $_p.strokeWeight(2);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20)
    }
    if (!lowGraphics) {
      $_p.strokeWeight(1);
      $_p.stroke(0, 0, 0);
      $_p.fill(168, 54, 5);
      $_p.ellipse(x - 12, y + 2, 5, 7);
      $_p.ellipse(x + 12, y + 2, 5, 7);
      $_p.noStroke();
      $_p.fill(232, 138, 30);
      $_p.ellipse(x - 11.5, y + 1, 4, 4);
      $_p.ellipse(x + 11.5, y + 1, 4, 4)
    }
    $_p.fill(199, 115, 18);
    $_p.stroke(0, 0, 0);
    $_p.ellipse(x + 10, y - 5, 4, 10);
    $_p.stroke(51, 19, 0);
    $_p.fill(105, 34, 3);
    $_p.arc(x, y + 1, 23, 23, -1, 182);
    $_p.rect(x - 12, y - 3, 23, 4);
    $_p.noStroke();
    $_p.rect(x - 11, y - 3, 22, 6);
    $_p.fill(168, 54, 5);
    $_p.rect(x - 11, y - 3, 19.5, 6);
    $_p.arc(x - 1, y + 1, 20, 21, -1, 182);
    $_p.stroke(0, 0, 0);
    $_p.fill(204, 102, 0);
    $_p.arc(x, y - 2, 23, 12, 180, 360);
    $_p.stroke(0, 0, 0);
    $_p.fill(232, 131, 54);
    $_p.arc(x, y - 1, 20, 18, 210, 330);
    $_p.noStroke();
    $_p.fill(168, 54, 5);
    if (!lowGraphics) {
      $_p.ellipse(x, y, 10, 10)
    }
    $_p.fill(0, 0, 0);
    $_p.ellipse(x + 6, y - 5.5, 2.3, 2.3);
    $_p.ellipse(x - 6, y - 5.5, 2.3, 2.3);
    $_p.pop()
  }
  
  function dart_monkey(x, y, r, ro) {
    $_p.noStroke();
    if (r) {
      $_p.fill(0, 0, 0, 60);
      $_p.ellipse(x, y, 100, 100)
    }
    if (!lowGraphics) {
      $_p.noFill();
      $_p.stroke(0, 0, 0);
      $_p.strokeWeight(4);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20);
      $_p.stroke(168, 54, 5);
      $_p.strokeWeight(2);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20)
    }
    if (!lowGraphics) {
      $_p.strokeWeight(1);
      $_p.stroke(0, 0, 0);
      $_p.fill(168, 54, 5);
      $_p.ellipse(x - 12, y + 2, 5, 7);
      $_p.ellipse(x + 12, y + 2, 5, 7);
      $_p.noStroke();
      $_p.fill(232, 138, 30);
      $_p.ellipse(x - 11.5, y + 1, 4, 4);
      $_p.ellipse(x + 11.5, y + 1, 4, 4)
    }
    $_p.fill(199, 115, 18);
    $_p.stroke(0, 0, 0);
    $_p.ellipse(x + 10, y - 5, 4, 10);
    $_p.stroke(51, 19, 0);
    $_p.fill(105, 34, 3);
    $_p.arc(x, y + 1, 23, 23, -1, 182);
    $_p.rect(x - 12, y - 3, 23, 4);
    $_p.noStroke();
    $_p.rect(x - 11, y - 3, 22, 6);
    $_p.fill(168, 54, 5);
    $_p.rect(x - 11, y - 3, 19.5, 6);
    $_p.arc(x - 1, y + 1, 20, 21, -1, 182);
    $_p.stroke(0, 0, 0);
    $_p.fill(204, 102, 0);
    $_p.arc(x, y - 2, 23, 12, 180, 360);
    $_p.stroke(0, 0, 0);
    $_p.fill(232, 131, 54);
    $_p.arc(x, y - 1, 20, 18, 210, 330);
    $_p.noStroke();
    $_p.fill(168, 54, 5);
    if (!lowGraphics) {
      $_p.ellipse(x, y, 10, 10)
    }
    $_p.fill(0, 0, 0);
    $_p.ellipse(x + 6, y - 5.5, 2.3, 2.3);
    $_p.ellipse(x - 6, y - 5.5, 2.3, 2.3)
  }
  
  function sentry(x, y, r, ro) {
    $_p.stroke(0, 0, 0);
    $_p.strokeWeight(3);
    $_p.line(x, y, x, y + 13);
    $_p.line(x, y, x + 9, y - 12);
    $_p.line(x, y, x - 9, y - 12);
    $_p.fill(0, 180, 0);
    $_p.noStroke();
    $_p.rect(x - 8, y - 8, 16, 16);
    $_p.fill(127.5, 127.5, 127.5);
    $_p.rect(x - 2, y - 15, 4, 8)
  }
  
  function engineer_monkey(x, y, r, ro) {
    $_p.noStroke();
    if (r) {
      $_p.fill(0, 0, 0, 60);
      $_p.ellipse(x, y, 180, 180)
    }
    if (!lowGraphics) {
      $_p.noFill();
      $_p.stroke(0, 0, 0);
      $_p.strokeWeight(4);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20);
      $_p.stroke(168, 54, 5);
      $_p.strokeWeight(2);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20)
    }
    if (!lowGraphics) {
      $_p.strokeWeight(1);
      $_p.stroke(0, 0, 0);
      $_p.fill(168, 54, 5);
      $_p.ellipse(x - 12, y + 2, 5, 7);
      $_p.ellipse(x + 12, y + 2, 5, 7);
      $_p.noStroke();
      $_p.fill(232, 138, 30);
      $_p.ellipse(x - 11.5, y + 1, 4, 4);
      $_p.ellipse(x + 11.5, y + 1, 4, 4)
    }
    $_p.fill(199, 190, 20);
    $_p.stroke(0, 0, 0);
    $_p.ellipse(x + 10, y - 10, 4, 36);
    $_p.ellipse(x + 10, y - 18, 12, 6);
    $_p.stroke(51, 19, 0);
    $_p.fill(105, 34, 3);
    $_p.arc(x, y + 1, 23, 23, -1, 182);
    $_p.rect(x - 12, y - 3, 23, 4);
    $_p.noStroke();
    $_p.rect(x - 11, y - 3, 22, 6);
    $_p.fill(168, 54, 5);
    $_p.rect(x - 11, y - 3, 19.5, 6);
    $_p.arc(x - 1, y + 1, 20, 21, -1, 182);
    $_p.stroke(0, 0, 0);
    $_p.fill(204, 102, 0);
    $_p.arc(x, y - 2, 23, 12, 180, 360);
    $_p.stroke(0, 0, 0);
    $_p.fill(232, 131, 54);
    $_p.arc(x, y - 1, 20, 18, 210, 330);
    $_p.noStroke();
    $_p.fill(168, 54, 5);
    if (!lowGraphics) {
      $_p.ellipse(x, y, 10, 10)
    }
    $_p.fill(0, 0, 0);
    $_p.ellipse(x + 6, y - 5.5, 2.3, 2.3);
    $_p.ellipse(x - 6, y - 5.5, 2.3, 2.3);
    $_p.fill(255, 196, 0);
    $_p.ellipse(x - 0.5, y + 3, 22, 18);
    $_p.fill(204, 139, 41);
    $_p.rect(x - 5, y - 4, 10, 14)
  }
  
  function sentry_monkey(x, y, r, ro) {
    $_p.noStroke();
    if (r) {
      $_p.fill(0, 0, 0, 60);
      $_p.ellipse(x, y, 180, 180)
    }
    if (!lowGraphics) {
      $_p.noFill();
      $_p.stroke(0, 0, 0);
      $_p.strokeWeight(4);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20);
      $_p.stroke(168, 54, 5);
      $_p.strokeWeight(2);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20)
    }
    if (!lowGraphics) {
      $_p.strokeWeight(1);
      $_p.stroke(0, 0, 0);
      $_p.fill(168, 54, 5);
      $_p.ellipse(x - 12, y + 2, 5, 7);
      $_p.ellipse(x + 12, y + 2, 5, 7);
      $_p.noStroke();
      $_p.fill(232, 138, 30);
      $_p.ellipse(x - 11.5, y + 1, 4, 4);
      $_p.ellipse(x + 11.5, y + 1, 4, 4)
    }
    $_p.fill(199, 190, 20);
    $_p.stroke(0, 0, 0);
    $_p.ellipse(x + 10, y - 10, 4, 36);
    $_p.ellipse(x + 10, y - 18, 12, 6);
    $_p.stroke(51, 19, 0);
    $_p.fill(105, 34, 3);
    $_p.arc(x, y + 1, 23, 23, -1, 182);
    $_p.rect(x - 12, y - 3, 23, 4);
    $_p.noStroke();
    $_p.rect(x - 11, y - 3, 22, 6);
    $_p.fill(168, 54, 5);
    $_p.rect(x - 11, y - 3, 19.5, 6);
    $_p.arc(x - 1, y + 1, 20, 21, -1, 182);
    $_p.stroke(0, 0, 0);
    $_p.fill(204, 102, 0);
    $_p.arc(x, y - 2, 23, 12, 180, 360);
    $_p.stroke(0, 0, 0);
    $_p.fill(232, 131, 54);
    $_p.arc(x, y - 1, 20, 18, 210, 330);
    $_p.noStroke();
    $_p.fill(168, 54, 5);
    if (!lowGraphics) {
      $_p.ellipse(x, y, 10, 10)
    }
    $_p.fill(0, 0, 0);
    $_p.ellipse(x + 6, y - 5.5, 2.3, 2.3);
    $_p.ellipse(x - 6, y - 5.5, 2.3, 2.3);
    $_p.fill(42, 140, 0);
    $_p.ellipse(x - 0.5, y + 3, 22, 18);
    $_p.fill(31, 110, 2);
    $_p.rect(x - 5, y - 4, 10, 14)
  }
  
  function ninja_monkey(x, y, r, ro) {
    $_p.noStroke();
    if (r) {
      $_p.fill(0, 0, 0, 60);
      $_p.ellipse(x, y, 180, 180)
    }
    if (!lowGraphics) {
      $_p.noFill();
      $_p.stroke(255, 0, 0);
      $_p.strokeWeight(4);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20);
      $_p.stroke(166, 5, 5);
      $_p.strokeWeight(2);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20)
    }
    if (!lowGraphics) {
      $_p.strokeWeight(1);
      $_p.stroke(255, 3, 3);
      $_p.fill(255, 0, 0);
      $_p.ellipse(x - 12, y + 2, 5, 7);
      $_p.ellipse(x + 12, y + 2, 5, 7);
      $_p.noStroke();
      $_p.fill(255, 13, 0);
      $_p.ellipse(x - 11.5, y + 1, 4, 4);
      $_p.ellipse(x + 11.5, y + 1, 4, 4)
    }
    $_p.fill(199, 37, 20);
    $_p.stroke(0, 0, 0);
    $_p.ellipse(x + 10, y - 5, 4, 10);
    $_p.stroke(51, 19, 0);
    $_p.fill(252, 0, 0);
    $_p.arc(x, y + 1, 23, 23, -1, 182);
    $_p.rect(x - 12, y - 3, 23, 4);
    $_p.noStroke();
    $_p.rect(x - 11, y - 3, 22, 6);
    $_p.fill(166, 5, 5);
    $_p.rect(x - 11, y - 3, 19.5, 6);
    $_p.arc(x - 1, y + 1, 20, 21, -1, 182);
    $_p.stroke(0, 0, 0);
    $_p.fill(204, 102, 0);
    $_p.arc(x, y - 2, 23, 12, 180, 360);
    $_p.stroke(0, 0, 0);
    $_p.fill(232, 131, 54);
    $_p.arc(x, y - 1, 20, 18, 210, 330);
    $_p.noStroke();
    $_p.fill(166, 5, 5);
    if (!lowGraphics) {
      $_p.ellipse(x, y, 10, 10)
    }
    $_p.fill(0, 0, 0);
    $_p.ellipse(x + 6, y - 5.5, 2.3, 2.3);
    $_p.ellipse(x - 6, y - 5.5, 2.3, 2.3)
  }
  
  function ninja_monkey10(x, y, r, ro) {
    $_p.noStroke();
    if (r) {
      $_p.fill(0, 0, 0, 60);
      $_p.ellipse(x, y, 225, 225)
    }
    if (!lowGraphics) {
      $_p.noFill();
      $_p.stroke(255, 255, 255);
      $_p.strokeWeight(4);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20);
      $_p.stroke(171, 171, 171);
      $_p.strokeWeight(2);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20)
    }
    if (!lowGraphics) {
      $_p.strokeWeight(1);
      $_p.stroke(255, 255, 255);
      $_p.fill(255, 255, 255);
      $_p.ellipse(x - 12, y + 2, 5, 7);
      $_p.ellipse(x + 12, y + 2, 5, 7);
      $_p.noStroke();
      $_p.fill(255, 255, 255);
      $_p.ellipse(x - 11.5, y + 1, 4, 4);
      $_p.ellipse(x + 11.5, y + 1, 4, 4)
    }
    $_p.fill(255, 255, 255);
    $_p.stroke(0, 0, 0);
    $_p.ellipse(x + 10, y - 5, 4, 10);
    $_p.stroke(51, 19, 0);
    $_p.fill(255, 255, 255);
    $_p.arc(x, y + 1, 23, 23, -1, 182);
    $_p.rect(x - 12, y - 3, 23, 4);
    $_p.noStroke();
    $_p.rect(x - 11, y - 3, 22, 6);
    $_p.fill(255, 255, 255);
    $_p.rect(x - 11, y - 3, 19.5, 6);
    $_p.arc(x - 1, y + 1, 20, 21, -1, 182);
    $_p.stroke(0, 0, 0);
    $_p.fill(204, 102, 0);
    $_p.arc(x, y - 2, 23, 12, 180, 360);
    $_p.stroke(0, 0, 0);
    $_p.fill(232, 131, 54);
    $_p.arc(x, y - 1, 20, 18, 210, 330);
    $_p.noStroke();
    $_p.fill(255, 255, 255);
    if (!lowGraphics) {
      $_p.ellipse(x, y, 10, 10)
    }
    $_p.fill(0, 0, 0);
    $_p.ellipse(x + 6, y - 5.5, 2.3, 2.3);
    $_p.ellipse(x - 6, y - 5.5, 2.3, 2.3)
  }
  
  function dart_monkey10(x, y, r, ro) {
    $_p.noStroke();
    if (r) {
      $_p.fill(0, 0, 0, 60);
      $_p.ellipse(x, y, 160, 160)
    }
    if (!lowGraphics) {
      $_p.noFill();
      $_p.stroke(0, 0, 0);
      $_p.strokeWeight(4);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20);
      $_p.stroke(168, 54, 5);
      $_p.strokeWeight(2);
      $_p.bezier(x, y + 13, x + 3, y + 22, x - 7, y + 10, x - 5, y + 20)
    }
    if (!lowGraphics) {
      $_p.strokeWeight(1);
      $_p.stroke(0, 0, 0);
      $_p.fill(168, 54, 5);
      $_p.ellipse(x - 12, y + 2, 5, 7);
      $_p.ellipse(x + 12, y + 2, 5, 7);
      $_p.noStroke();
      $_p.fill(232, 138, 30);
      $_p.ellipse(x - 11.5, y + 1, 4, 4);
      $_p.ellipse(x + 11.5, y + 1, 4, 4)
    }
    $_p.fill(199, 115, 18);
    $_p.stroke(0, 0, 0);
    $_p.ellipse(x + 10, y - 5, 4, 10);
    $_p.stroke(51, 19, 0);
    $_p.fill(105, 34, 3);
    $_p.arc(x, y + 1, 23, 23, -1, 182);
    $_p.rect(x - 12, y - 3, 23, 4);
    $_p.noStroke();
    $_p.rect(x - 11, y - 3, 22, 6);
    $_p.fill(168, 54, 5);
    $_p.rect(x - 11, y - 3, 19.5, 6);
    $_p.arc(x - 1, y + 1, 20, 21, -1, 182);
    $_p.stroke(0, 0, 0);
    $_p.fill(204, 102, 0);
    $_p.arc(x, y - 2, 23, 12, 180, 360);
    $_p.stroke(0, 0, 0);
    $_p.fill(232, 131, 54);
    $_p.arc(x, y - 1, 20, 18, 210, 330);
    $_p.noStroke();
    $_p.fill(168, 54, 5);
    if (!lowGraphics) {
      $_p.ellipse(x, y, 10, 10)
    }
    $_p.fill(0, 0, 0);
    $_p.ellipse(x + 6, y - 5.5, 2.3, 2.3);
    $_p.ellipse(x - 6, y - 5.5, 2.3, 2.3);
    $_p.strokeWeight(3);
    $_p.stroke(255, 0, 0);
    $_p.line(x - 8, y + 6, x + 8, y + 6)
  }
  
  function super_monkey(x, y, r) {
    $_p.noStroke();
    if (r) {
      $_p.fill(0, 0, 0, 60);
      $_p.ellipse(x, y, 180, 180)
    }
    if (!lowGraphics) {
      $_p.fill(189, 0, 0);
      $_p.arc(x, y + 4, 22, 30, 0, 180);
      $_p.fill(255, 0, 0);
      $_p.arc(x - 1, y + 4, 20, 28, 0, 180);
      $_p.fill(255, 213, 0);
      $_p.rect(x - 3, y + 13, 6, 3, 1)
    }
    if (!lowGraphics) {
      $_p.strokeWeight(1);
      $_p.stroke(0, 0, 0);
      $_p.fill(0, 115, 255);
      $_p.ellipse(x - 12, y + 2, 5, 7);
      $_p.ellipse(x + 12, y + 2, 5, 7);
      $_p.noStroke();
      $_p.fill(232, 138, 30);
      $_p.ellipse(x - 11.5, y + 1, 4, 4);
      $_p.ellipse(x + 11.5, y + 1, 4, 4)
    }
    $_p.fill(199, 115, 18);
    $_p.stroke(0, 0, 0);
    $_p.ellipse(x + 10, y - 5, 4, 10);
    $_p.stroke(51, 19, 0);
    $_p.fill(0, 69, 148);
    $_p.arc(x, y + 1, 23, 23, -1, 182);
    $_p.rect(x - 12, y - 3, 23, 4);
    $_p.noStroke();
    $_p.rect(x - 11, y - 3, 22, 6);
    $_p.stroke(51, 19, 0);
    $_p.fill(189, 220, 255);
    $_p.arc(x, y + 1, 23, 23, 96, 182);
    $_p.rect(x - 12, y - 3, 20, 4);
    $_p.noStroke();
    $_p.rect(x - 11, y - 3, 20, 6);
    $_p.fill(0, 115, 255);
    $_p.rect(x - 9, y - 3, 18, 6);
    $_p.arc(x, y + 1, 18, 22, -1, 182);
    $_p.stroke(0, 0, 0);
    $_p.fill(204, 102, 0);
    $_p.arc(x, y - 2, 23, 12, 180, 360);
    $_p.stroke(0, 0, 0);
    $_p.fill(232, 131, 54);
    $_p.arc(x, y - 1, 20, 18, 210, 330);
    $_p.noStroke();
    $_p.fill(0, 115, 255);
    $_p.ellipse(x, y, 10, 10);
    $_p.fill(0, 0, 0);
    $_p.ellipse(x + 6, y - 5.5, 2.3, 2.3);
    $_p.ellipse(x - 6, y - 5.5, 2.3, 2.3)
  }
  
  function super_monkey01(x, y, r) {
    $_p.noStroke();
    if (r) {
      $_p.fill(0, 0, 0, 60);
      $_p.ellipse(x, y, 200, 200)
    }
    if (!lowGraphics) {
      $_p.fill(189, 0, 0);
      $_p.arc(x, y + 4, 22, 30, 0, 180);
      $_p.fill(255, 0, 0);
      $_p.arc(x - 1, y + 4, 20, 28, 0, 180);
      $_p.fill(255, 213, 0);
      $_p.rect(x - 3, y + 13, 6, 3, 1)
    }
    if (!lowGraphics) {
      $_p.strokeWeight(1);
      $_p.stroke(0, 0, 0);
      $_p.fill(0, 115, 255);
      $_p.ellipse(x - 12, y + 2, 5, 7);
      $_p.ellipse(x + 12, y + 2, 5, 7);
      $_p.noStroke();
      $_p.fill(232, 138, 30);
      $_p.ellipse(x - 11.5, y + 1, 4, 4);
      $_p.ellipse(x + 11.5, y + 1, 4, 4)
    }
    $_p.fill(199, 115, 18);
    $_p.stroke(0, 0, 0);
    $_p.ellipse(x + 10, y - 5, 6, 15);
    $_p.stroke(51, 19, 0);
    $_p.fill(0, 69, 148);
    $_p.arc(x, y + 1, 23, 23, -1, 182);
    $_p.rect(x - 12, y - 3, 23, 4);
    $_p.noStroke();
    $_p.rect(x - 11, y - 3, 22, 6);
    $_p.stroke(51, 19, 0);
    $_p.fill(189, 220, 255);
    $_p.arc(x, y + 1, 23, 23, 96, 182);
    $_p.rect(x - 12, y - 3, 20, 4);
    $_p.noStroke();
    $_p.rect(x - 11, y - 3, 20, 6);
    $_p.fill(0, 115, 255);
    $_p.rect(x - 9, y - 3, 18, 6);
    $_p.arc(x, y + 1, 18, 22, -1, 182);
    $_p.stroke(0, 0, 0);
    $_p.fill(204, 102, 0);
    $_p.arc(x, y - 2, 23, 12, 180, 360);
    $_p.stroke(0, 0, 0);
    $_p.fill(232, 131, 54);
    $_p.arc(x, y - 1, 20, 18, 210, 330);
    $_p.noStroke();
    $_p.fill(0, 115, 255);
    $_p.ellipse(x, y, 10, 10);
    $_p.stroke(0, 0, 0);
    $_p.strokeWeight(0.5);
    $_p.fill(34, 135, 0);
    $_p.ellipse(x + 6, y - 5.5, 2.8, 2.8);
    $_p.ellipse(x - 6, y - 5.5, 2.8, 2.8)
  }
  
  function banana_farm(x, y, r) {
    $_p.noStroke();
    if (r) {
      $_p.fill(0, 0, 0, 60);
      $_p.ellipse(x, y, 60, 60)
    }
    $_p.stroke(0, 0, 0);
    $_p.fill(191, 126, 73);
    $_p.rect(x - 15, y - 15, 30, 30);
    $_p.noFill();
    $_p.strokeWeight(2);
    $_p.stroke(82, 51, 12);
    $_p.rect(x - 10, y - 9, 20, 20);
    if (!lowGraphics) {
      $_p.stroke(138, 85, 17);
      $_p.rect(x - 11, y - 11, 22, 22);
      $_p.stroke(133, 83, 17);
      $_p.rect(x - 12, y - 12, 24, 24)
    }
    $_p.stroke(247, 255, 0);
    $_p.strokeWeight(5);
    $_p.arc(x - 5, y - 10, 20, 30, 10, 80);
    $_p.strokeWeight(1)
  }
  
  function factory(x, y, r) {
    $_p.push();
    $_p.translate(x, y);
    $_p.scale(1.4);
    $_p.translate(-x, -y);
    if (r) {
      $_p.fill(0, 0, 0, 60)
    }
    $_p.noStroke();
    $_p.fill(140, 140, 140);
    $_p.rect(x - 16, y - 16, 32, 32);
    $_p.fill(99, 147, 171);
    $_p.ellipse(x, y, 1, 1);
    $_p.beginShape();
    $_p.vertex(x - 13, y + 13);
    $_p.vertex(x + 13, y + 13);
    $_p.vertex(x + 13, y - 2);
    $_p.vertex(x + 2, y - 2);
    $_p.vertex(x + 2, y - 18);
    $_p.vertex(x - 13, y - 2);
    $_p.endShape();
    $_p.fill(110, 173, 184);
    $_p.beginShape();
    $_p.vertex(x + 2, y - 17);
    $_p.vertex(x + 2, y - 32);
    $_p.vertex(x - 13, y - 17);
    $_p.vertex(x - 13, y - 2);
    $_p.endShape();
    $_p.rect(x + 2, y - 19, 11, 17);
    $_p.fill(201, 171, 0);
    $_p.rect(x - 9, y + 5, 8, 3);
    $_p.rect(x + 2, y + 5, 8, 3);
    $_p.fill(99, 147, 171);
    $_p.stroke(110, 173, 184);
    $_p.ellipse(x + 7.5, y - 14, 10, 8);
    $_p.ellipse(x + 7.5, y - 6, 10, 8);
    $_p.beginShape();
    $_p.vertex(x + 2, y - 14);
    $_p.vertex(x + 3.5, y - 24);
    $_p.vertex(x + 11.5, y - 24);
    $_p.vertex(x + 13, y - 14);
    $_p.endShape();
    $_p.ellipse(x + 7.5, y - 24, 6, 2);
    $_p.beginShape();
    $_p.vertex(x + 2, y - 6);
    $_p.vertex(x + 3.5, y - 16);
    $_p.vertex(x + 11.5, y - 16);
    $_p.vertex(x + 13, y - 6);
    $_p.endShape();
    $_p.ellipse(x + 7.5, y - 16, 6, 2);
    $_p.pop();
    $_p.noFill();
    $_p.stroke(247, 255, 0);
    $_p.strokeWeight(5);
    $_p.arc(x - 14, y - 33, 20, 30, 20, 80);
    $_p.strokeWeight(1)
  }
  
  function cannon(x, y, r) {
    if (r) {
      $_p.fill(0, 0, 0, 60);
      $_p.ellipse(x, y, 200, 200)
    }
    $_p.push();
    $_p.translate(0, 10);
    $_p.fill(0, 0, 0);
    $_p.noStroke();
    $_p.ellipse(x, y, 20, 20);
    $_p.quad(x - 10, y, x + 10, y, x + 6.5, y - 20, x - 6.5, y - 20);
    $_p.fill(51, 24, 4);
    $_p.rect(x - 12.5, y - 11, 3, 18);
    $_p.rect(x + 8.5, y - 11, 3, 18);
    $_p.stroke(117, 117, 117);
    $_p.strokeWeight(2);
    $_p.line(x + 4.5, y - 18, x + 7, y - 1);
    $_p.stroke(0, 0, 0);
    $_p.line(x + 7, y - 20, x - 7, y - 20);
    $_p.pop()
  }
  
  function spikes(x, y) {
    $_p.stroke(255, 255, 255);
    $_p.fill(0, 0, 0);
    $_p.triangle(x, y - 20, x - 2, y + 20, x + 2, y + 20);
    $_p.triangle(x + 20, y, x - 2, y + 20, x + 2, y + 20);
    $_p.triangle(x - 20, y, x - 2, y + 20, x + 2, y + 20)
  }
  
  function draw_dart(x, y, isNinja) {
    if (isNinja === 500 || isNinja === 750) {
      $_p.stroke(0, 0, 0);
      $_p.strokeWeight(3);
      $_p.line(x, y - 10, x, y);
      $_p.noStroke()
    }
    if (isNinja === 22) {
      $_p.stroke(0, 0, 0);
      $_p.fill(15, 15, 15);
      $_p.strokeWeight(6);
      $_p.line(x - 16, y + 10, x + 16, y + 10);
      $_p.line(x, y - 5, x, y + 25);
      $_p.strokeWeight(7);
      $_p.triangle(x - 6, y + 3, x - 6, y + 8, x - 10, y + 2);
      $_p.triangle(x - 6, y + 18, x - 6, y + 12, x - 10, y + 18);
      $_p.triangle(x - 6, y + 3, x - 6, y + 8, x + 5.2, y + 2);
      $_p.triangle(x + 8, y + 18, x + 8, y + 12, x + 12, y + 18);
      $_p.ellipse(x, y + 10, 20, 20);
      $_p.noStroke();
      return
    }
    if (isNinja === 1000) {
      $_p.push();
      $_p.translate(x - 70, y - 75);
      $_p.scale(0.5);
      $_p.fill(92, 92, 92);
      $_p.triangle(145, 159, 170, 184, 140, 173);
      $_p.triangle(111, 188, 136, 159, 140, 173);
      $_p.triangle(116, 137, 129, 164, 146, 163);
      $_p.triangle(166, 134, 133, 164, 150, 163);
      $_p.fill(255, 255, 255);
      $_p.ellipse(138, 165, 11, 11);
      $_p.pop()
    } else if (isNinja !== 500 && isNinja !== 99 && isNinja !== 750 && isNinja !== 22 && isNinja !== -2.3 && isNinja !== -2.4) {
      if (lowGraphics) {
        $_p.fill(0, 0, 0);
        $_p.triangle(x - 4, y + 10, x + 4, y + 10, x, y - 10)
      } else {
        $_p.strokeWeight(1.5);
        $_p.stroke(0, 0, 0);
        $_p.fill(255, 255, 255);
        $_p.ellipse(x, y, 6, 12);
        $_p.noStroke();
        $_p.fill(105, 105, 105);
        $_p.ellipse(x + 1, y, 3, 12);
        $_p.fill(0, 0, 0);
        $_p.rect(x - 1, y - 6, 1.5, -4);
        $_p.fill(0, 0, 0);
        $_p.rect(x - 1, y + 6, 1.5, 6);
        $_p.strokeWeight(1);
        $_p.stroke(0, 0, 0);
        $_p.fill(225, 255, 0);
        $_p.rect(x - 4, y + 9, 7, 3)
      }
    }
    if (isNinja === 99) {
      $_p.push();
      $_p.translate(x - 70, y - 75);
      $_p.scale(0.5);
      $_p.fill(255, 0, 0);
      $_p.triangle(145, 159, 170, 184, 140, 173);
      $_p.triangle(111, 188, 136, 159, 140, 173);
      $_p.triangle(116, 137, 129, 164, 146, 163);
      $_p.triangle(166, 134, 133, 164, 150, 163);
      $_p.fill(158, 38, 191);
      $_p.ellipse(138, 165, 11, 11);
      $_p.pop()
    }
    if (isNinja === -2.4 || isNinja === -2.3) {
      $_p.fill(0, 0, 0);
      $_p.ellipse(x, y, 20, 20);
      $_p.noFill();
      $_p.strokeWeight(2);
      $_p.stroke(0, 0, 0);
      $_p.bezier(x, y - 10, x + 1.5, y - 16, x + 4.5, y - 10, x + 6, y - 15);
      $_p.stroke(122, 122, 122);
      $_p.arc(x, y, 15, 15, -75, 0);
      $_p.fill(127, 127, 127);
      $_p.noStroke()
    }
  }
  
  function ee() {
  }
  
  function getSpeed(t1) {
    if (t1 < 6) {
      return 1.6 + t1 * 0.32
    }
    if (t1 === 6 || t1 === 7) {
      return 2.5
    }
    if (t1 === 8) {
      return 2.9
    }
    return $_p.pow(1.024, $_p.max((frames - 5000) / 15, 0))
  }
  
  function MOAB(x, y, R) {
    $_p.push();
    if (R === 0) {
      $_p.rotate(90);
      $_p.translate(1 * (y - 200), -1 * (x + 250))
    }
    if (R === 90) {
      $_p.rotate(180);
      $_p.translate(-1 * (x + 200), -1 * (y + 250))
    }
    if (R === 180) {
      $_p.rotate(270);
      $_p.translate(-1 * (y + 200), 1 * (x - 250))
    }
    if (R === 270) {
      $_p.rotate(0);
      $_p.translate(1 * (x - 200), 1 * (y - 200))
    }
    $_p.fill(14, 96, 158);
    $_p.ellipse(215, 197, 111, 170);
    $_p.fill(54, 103, 201);
    $_p.curve(315, 26, 196, 273, 184, 125, 258, 122);
    $_p.curve(315, 49, 211, 281, 199, 117, 258, 122);
    $_p.line(215, 110, 215, 283);
    $_p.arc(217, 175, 40, 251, -27, 54);
    $_p.arc(225, 184, 67, 205, -27, 54);
    $_p.fill(122, 57, 57);
    $_p.ellipse(190, 230, 30, 30);
    $_p.ellipse(232, 170, 30, 30);
    $_p.ellipse(232, 230, 30, 30);
    $_p.ellipse(190, 170, 30, 30);
    $_p.push();
    $_p.fill(94, 79, 79);
    $_p.push();
    $_p.scale(1.02, 1.02);
    $_p.rotate(-0.5);
    $_p.translate(2, 2);
    $_p.triangle(185, 267, 135, 277, 165, 231);
    $_p.pop();
    $_p.translate(-1, -1);
    $_p.rotate(268.5);
    $_p.translate(-429.8, -15);
    $_p.triangle(175, 267, 130, 277, 150, 241);
    $_p.pop();
    $_p.pop()
  }
  
  function red_balloon(x, y) {
    if (lowGraphics) {
      $_p.fill(255, 0, 0);
      $_p.arc(x + 1, y, 13, 25, 0, 180);
      $_p.arc(x + 1, y, 13, 15, 180, 360);
      $_p.ellipse(x + 1, y + 13, 5, 3)
    } else {
      $_p.noStroke();
      $_p.fill(145, 0, 0);
      $_p.ellipse(x, y, 15, 15);
      $_p.arc(x, y, 15, 30, 0, 180);
      $_p.ellipse(x, y + 16, 5, 2);
      $_p.fill(255, 0, 0);
      $_p.ellipse(x, y, 12, 15);
      $_p.arc(x + 1, y, 13, 25, 0, 180);
      $_p.fill(255, 255, 255);
      $_p.ellipse(x + 2, y, 10, 12);
      $_p.fill(255, 0, 0);
      $_p.ellipse(x - 0.5, y + 1, 10, 12)
    }
  }
  
  function blue_balloon(x, y) {
    if (lowGraphics) {
      $_p.fill(0, 153, 255);
      $_p.arc(x + 1, y, 13, 25, 0, 180);
      $_p.arc(x + 1, y, 13, 15, 180, 360);
      $_p.ellipse(x + 1, y + 13, 5, 3)
    } else {
      $_p.noStroke();
      $_p.fill(0, 75, 255);
      $_p.ellipse(x, y, 15, 15);
      $_p.arc(x, y, 15, 30, 0, 180);
      $_p.ellipse(x, y + 16, 5, 2);
      $_p.fill(0, 153, 255);
      $_p.ellipse(x, y, 12, 15);
      $_p.arc(x + 1, y, 13, 25, 0, 180);
      $_p.fill(255, 255, 255);
      $_p.ellipse(x + 2, y, 10, 12);
      $_p.fill(0, 153, 255);
      $_p.ellipse(x - 0.5, y + 1, 10, 12)
    }
  }
  
  function green_balloon(x, y) {
    if (lowGraphics) {
      $_p.fill(0, 224, 0);
      $_p.arc(x + 1, y, 13, 25, 0, 180);
      $_p.arc(x + 1, y, 13, 15, 180, 360);
      $_p.ellipse(x + 1, y + 13, 5, 3)
    } else {
      $_p.noStroke();
      $_p.fill(0, 133, 0);
      $_p.ellipse(x, y, 15, 15);
      $_p.arc(x, y, 15, 30, 0, 180);
      $_p.ellipse(x, y + 16, 5, 2);
      $_p.fill(0, 224, 0);
      $_p.ellipse(x, y, 12, 15);
      $_p.arc(x + 1, y, 13, 25, 0, 180);
      $_p.fill(255, 255, 255);
      $_p.ellipse(x + 2, y, 10, 12);
      $_p.fill(0, 224, 0);
      $_p.ellipse(x - 0.5, y + 1, 10, 12)
    }
  }
  
  function yellow_balloon(x, y) {
    if (lowGraphics) {
      $_p.fill(235, 247, 0);
      $_p.arc(x + 1, y, 13, 25, 0, 180);
      $_p.arc(x + 1, y, 13, 15, 180, 360);
      $_p.ellipse(x + 1, y + 13, 5, 3)
    } else {
      $_p.noStroke();
      $_p.fill(126, 143, 49);
      $_p.ellipse(x, y, 15, 15);
      $_p.arc(x, y, 15, 30, 0, 180);
      $_p.ellipse(x, y + 16, 5, 2);
      $_p.fill(235, 247, 0);
      $_p.ellipse(x, y, 12, 15);
      $_p.arc(x + 1, y, 13, 25, 0, 180);
      $_p.fill(255, 255, 255);
      $_p.ellipse(x + 2, y, 10, 12);
      $_p.fill(213, 255, 5);
      $_p.ellipse(x - 0.5, y + 1, 10, 12)
    }
  }
  
  function pink_balloon(x, y) {
    if (lowGraphics) {
      $_p.fill(242, 162, 238);
      $_p.arc(x + 1, y, 13, 25, 0, 180);
      $_p.arc(x + 1, y, 13, 15, 180, 360);
      $_p.ellipse(x + 1, y + 13, 5, 3)
    } else {
      $_p.noStroke();
      $_p.fill(186, 112, 171);
      $_p.ellipse(x, y, 15, 15);
      $_p.arc(x, y, 15, 30, 0, 180);
      $_p.ellipse(x, y + 16, 5, 2);
      $_p.fill(242, 162, 238);
      $_p.ellipse(x, y, 12, 15);
      $_p.arc(x + 1, y, 13, 25, 0, 180);
      $_p.fill(255, 255, 255);
      $_p.ellipse(x + 2, y, 10, 12);
      $_p.fill(242, 162, 238);
      $_p.ellipse(x - 0.5, y + 1, 10, 12)
    }
  }
  
  function black_balloon(x, y) {
    if (lowGraphics) {
      $_p.fill(0, 0, 0);
      $_p.arc(x + 1, y, 13, 25, 0, 180);
      $_p.arc(x + 1, y, 13, 15, 180, 360);
      $_p.ellipse(x + 1, y + 13, 5, 3)
    } else {
      $_p.noStroke();
      $_p.fill(0, 0, 0);
      $_p.ellipse(x, y, 15, 15);
      $_p.arc(x, y, 15, 30, 0, 180);
      $_p.ellipse(x, y + 16, 5, 2);
      $_p.fill(20, 19, 20);
      $_p.ellipse(x, y, 12, 15);
      $_p.arc(x + 1, y, 13, 25, 0, 180);
      $_p.fill(255, 255, 255);
      $_p.ellipse(x + 2, y, 10, 12);
      $_p.fill(69, 69, 69);
      $_p.ellipse(x - 0.5, y + 1, 10, 12)
    }
  }
  
  function white_balloon(x, y) {
    if (lowGraphics) {
      $_p.fill(255, 252, 255);
      $_p.arc(x + 1, y, 13, 25, 0, 180);
      $_p.arc(x + 1, y, 13, 15, 180, 360);
      $_p.ellipse(x + 1, y + 13, 5, 3)
    } else {
      $_p.noStroke();
      $_p.fill(255, 255, 255);
      $_p.ellipse(x, y, 15, 15);
      $_p.arc(x, y, 15, 30, 0, 180);
      $_p.ellipse(x, y + 16, 5, 2);
      $_p.fill(196, 196, 196);
      $_p.ellipse(x, y, 12, 15);
      $_p.arc(x + 1, y, 13, 25, 0, 180);
      $_p.fill(255, 255, 255);
      $_p.ellipse(x + 2, y, 10, 12);
      $_p.fill(255, 255, 255);
      $_p.ellipse(x - 0.5, y + 1, 10, 12)
    }
  }
  
  function cer_balloon(x, y) {
    if (lowGraphics) {
      $_p.fill(133, 53, 53);
      $_p.arc(x + 1, y, 13, 25, 0, 180);
      $_p.arc(x + 1, y, 13, 15, 180, 360);
      $_p.ellipse(x + 1, y + 13, 5, 3)
    } else {
      $_p.noStroke();
      $_p.fill(158, 115, 115);
      $_p.ellipse(x, y, 15, 15);
      $_p.arc(x, y, 15, 30, 0, 180);
      $_p.ellipse(x, y + 16, 5, 2);
      $_p.fill(163, 68, 68);
      $_p.ellipse(x, y, 12, 15);
      $_p.arc(x + 1, y, 13, 25, 0, 180);
      $_p.fill(255, 255, 255);
      $_p.ellipse(x + 2, y, 10, 12);
      $_p.fill(145, 77, 77);
      $_p.ellipse(x - 0.5, y + 1, 10, 12)
    }
  }
  
  function drawBalloon(x, y, t, R) {
    switch (t) {
    case 1:
      red_balloon(x, y);
      break;
    case 2:
      blue_balloon(x, y);
      break;
    case 3:
      green_balloon(x, y);
      break;
    case 4:
      yellow_balloon(x, y);
      break;
    case 5:
      pink_balloon(x, y);
      break;
    case 6:
      black_balloon(x, y);
      break;
    case 7:
      white_balloon(x, y);
      break;
    case 8:
      cer_balloon(x, y);
      break;
    case 9:
      MOAB(x, y, R);
      break
    }
  }
  
  function draw_pop(x, y, r) {
    $_p.translate(x, y);
    $_p.rotate(r);
    $_p.translate(-5, -7);
    $_p.strokeWeight(2);
    $_p.stroke(0, 0, 0);
    $_p.fill(255, 255, 255);
    $_p.triangle(15, -3, -7, 0, 6, 24);
    $_p.triangle(18, 8, -3, 24, 0, 0);
    $_p.triangle(12, 18, -9, 11, 9, -7);
    $_p.noStroke();
    $_p.fill(255, 255, 255);
    $_p.triangle(15, -3, -7, 0, 6, 24);
    $_p.triangle(18, 8, -3, 24, 0, 0);
    $_p.triangle(12, 18, -9, 11, 9, -7);
    $_p.resetMatrix();
    $_p.strokeWeight(1)
  }
  
  function draw_expl(x, y, r, radius) {
    $_p.translate(x, y);
    $_p.rotate(r);
    $_p.scale(radius / 10);
    $_p.translate(-5, -7);
    $_p.strokeWeight(2);
    $_p.stroke(0, 0, 0);
    $_p.fill(255, 145, 0);
    $_p.triangle(15, -3, -7, 0, 6, 24);
    $_p.triangle(18, 8, -3, 24, 0, 0);
    $_p.triangle(12, 18, -9, 11, 9, -7);
    $_p.noStroke();
    $_p.fill(255, 94, 0);
    $_p.triangle(15, -3, -7, 0, 6, 24);
    $_p.triangle(18, 8, -3, 24, 0, 0);
    $_p.triangle(12, 18, -9, 11, 9, -7);
    $_p.resetMatrix();
    $_p.strokeWeight(1)
  }
  
  function make_pop(x, y) {
    pops.push({
      x: x,
      y: y,
      w: 0,
      t: 1,
      r: $_p.random(0, 360)
    })
  }
  
  function make_expl(x, y, d, r) {
    expl.push({
      x: x,
      y: y,
      w: 0,
      t: d,
      radius: r,
      r: $_p.random(0, 360)
    })
  }
  
  function update_pop() {
    for (var i = 0; i < pops.length; i += 1) {
      draw_pop(pops[i].x, pops[i].y, pops[i].r);
      pops[i].w += 1;
      if (pops[i].w > pops[i].t) {
        pops.splice(i, 1)
      }
    }
  }
  
  function update_expl() {
    for (var i = 0; i < expl.length; i += 1) {
      draw_expl(expl[i].x, expl[i].y, expl[i].r, expl[i].radius);
      expl[i].w += 1;
      for (var x1 = 0; x1 < bal.length; x1++) {
        if ($_p.dist(bal[x1].x, bal[x1].y, expl[i].x, expl[i].y) < expl[i].radius) {
          if (bal[x1].type < 8) {
            make_pop(bal[x1].x, bal[x1].y);
            bloonPop += 1;
            money += 1;
            if (bal[x1].type < 6) {
              bal[x1].type -= 1;
              bal[x1].speed = getSpeed(bal[i].type - 1)
            }
            if (bal[x1].type > 6) {
              bal[x1].type -= 2;
              bal[x1].speed = getSpeed(bal[i].type - 2)
            }
          }
          if (bal[x1].type === 8) {
            bal[i].hp--;
            if (bal[i].hp <= 0) {
              bal[i].type--;
              money++;
              bal.push({
                x: bal[i].x + 3,
                y: bal[i].y,
                ph: bal[i].ph,
                speed: getSpeed(7),
                type: 7
              });
              bal.push({
                x: bal[i].x + 1,
                y: bal[i].y,
                ph: bal[i].ph,
                speed: getSpeed(6),
                type: 6
              });
              bal.push({
                x: bal[i].x - 1,
                y: bal[i].y,
                ph: bal[i].ph,
                speed: getSpeed(6),
                type: 6
              })
            }
          }
          if (bal[x1].type === 9) {
            bal[i].hp--;
            if (bal[i].hp <= 0) {
              bal[i].type--;
              bal[i].x -= 2;
             // playSound(getSound('retro/boom1'));
              bal.push({
                x: bal[i].x + 2,
                y: bal[i].y,
                ph: bal[i].ph,
                type: 8,
                speed: getSpeed(8),
                hp: 9
              })
            }
          }
        }
      }
      if (expl[i].w > expl[i].t) {
        expl.splice(i, 1)
      }
    }
  }
  
  function update_balloons() {
    for (var i = 0; i < bal.length; i += 1) {
      if (bal[i].type !== 9) {
        drawBalloon(bal[i].x, bal[i].y, bal[i].type, 0)
      } else {
        if (bal[i].ph === 'left') {
          drawBalloon(bal[i].x, bal[i].y, bal[i].type, 0)
        }
        if (bal[i].ph === 'right') {
          drawBalloon(bal[i].x, bal[i].y, bal[i].type, 180)
        }
        if (bal[i].ph === 'up') {
          drawBalloon(bal[i].x, bal[i].y, bal[i].type, 270)
        }
        if (bal[i].ph === 'down') {
          drawBalloon(bal[i].x, bal[i].y, bal[i].type, 90)
        }
      }
      if (bal[i].ph === 'left') {
        bal[i].x += bal[i].speed
      } else if (bal[i].ph === 'right') {
        bal[i].x -= bal[i].speed
      } else if (bal[i].ph === 'down') {
        bal[i].y += bal[i].speed
      } else if (bal[i].ph === 'up') {
        bal[i].y -= bal[i].speed
      }
      if (mapid === 0) {
        if ($_p.random(0, 4) > 2) {
          bal[i].ph = 'up'
        } else {
          bal[i].ph = 'down'
        }
        bal[i].ph = 'left';
        bal[i].ph = 'left';
        bal[i].ph = 'down';
        bal[i].ph = 'up';
        bal[i].ph = 'left';
        if (bal[i].x > 55 && bal[i].x < 80 && bal[i].ph === 'left') {
          bal[i].ph = 'up'
        }
        if (bal[i].y < 110 && bal[i].x < 100 && bal[i].ph === 'up') {
          bal[i].ph = 'left'
        }
        if (bal[i].x > 165 && bal[i].x < 200 && bal[i].ph === 'left') {
          bal[i].ph = 'down'
        }
        if (bal[i].y > 285 && bal[i].y < 340 && bal[i].ph === 'down') {
          bal[i].ph = 'left'
        }
        if (bal[i].x > 286 && bal[i].x < 315 && bal[i].ph === 'left') {
          bal[i].ph = 'up'
        }
        if (bal[i].y < 200 && bal[i].x > 250 && bal[i].ph === 'up') {
          bal[i].ph = 'left'
        }
        if (bal[i].x < 400 && bal[i].x > 384 && bal[i].ph === 'left') {
          bal[i].ph = 'down'
        }
        if (bal[i].y > 388 && bal[i].y < 410 && bal[i].ph === 'down') {
          bal[i].ph = 'left'
        }
      }
      if (mapid === 1) {
        if (bal[i].x > 50 && bal[i].y < 290) {
          bal[i].ph = 'down'
        }
        if (bal[i].x > 50 && bal[i].y > 290) {
          bal[i].ph = 'left'
        }
      }
      for (var ww = 0; ww < dart.length; ww += 1) {
        var d = 15;
        var d2 = 30;
        if ($_p.dist(dart[ww].x, dart[ww].y, bal[i].x, bal[i].y) < d || (dart[ww].isNinja === 22 || dart[ww].isNinja === 1000 || dart[ww].isNinja === 99 || dart[ww].isNinja === -2.3 || dart[ww].isNinja === -2.4) && $_p.dist(dart[ww].x, dart[ww].y, bal[i].x, bal[i].y) < d2) {
          if (dart[ww].isNinja === -2.3) {
            make_expl(dart[ww].x, dart[ww].y, 0, 60)
          }
          if (dart[ww].isNinja === -2.4) {
            make_expl(dart[ww].x, dart[ww].y, 2, 90)
          }
          if (bal[i].type < 8) {
            make_pop(bal[i].x, bal[i].y);
            bloonPop += 1;
            money++;
            if (bal[i].type < 7) {
              bal[i].type -= 1;
              bal[i].speed = getSpeed(bal[i].type - 1)
            } else {
              bal[i].type -= 2;
              bal[i].speed = getSpeed(bal[i].type - 2)
            }
          }
          if (bal[i].type === 8) {
            if (dart[ww].isNinja === 22) {
              bal[i].hp -= 3
            }
            bal[i].hp--;
            if (bal[i].hp <= 0) {
              money++;
              bal[i].type--;
              bal.push({
                x: bal[i].x + 3,
                y: bal[i].y,
                ph: bal[i].ph,
                speed: getSpeed(7),
                type: 7
              });
              bal.push({
                x: bal[i].x + 1,
                y: bal[i].y,
                ph: bal[i].ph,
                speed: getSpeed(6),
                type: 6
              });
              bal.push({
                x: bal[i].x - 1,
                y: bal[i].y,
                ph: bal[i].ph,
                speed: getSpeed(6),
                type: 6
              })
            }
          }
          if (bal[i].type === 9) {
            money += 2;
            if (dart[ww].isNinja === 22) {
            }
            bal[i].hp--;
            if (bal[i].hp <= 0) {
              bal[i].type--;
              money++;
              bal[i].x -= 2;
             // playSound(getSound('retro/boom1'));
              bal.push({
                x: bal[i].x + 2,
                y: bal[i].y,
                ph: bal[i].ph,
                type: 8,
                speed: getSpeed(8),
                hp: 9
              })
            }
          }
          if (dart[ww].cap !== 1) {
            dart[ww].cap -= 1
          } else {
            dart.splice(ww, 1)
          }
        }
      }
      if (bal[i].type < 1) {
        bal.splice(i, 1)
      } else if (bal[i].x > 400) {
        if (bal[i].type === 8) {
          lives -= 17
        }
        if (bal[i].type === 9) {
          lives = 9
        }
        lives -= bal[i].type
        bal.splice(i, 1);
      }
    }
  }
  
  function track() {
    if (mapid === 0) {
      $_p.fill(74, 74, 74);
      $_p.rect(0, 200, 40, 30);
      $_p.rect(40, 110, 30, 120);
      $_p.rect(40, 100, 120, 30);
      $_p.rect(150, 100, 30, 180);
      $_p.rect(150, 280, 120, 30);
      $_p.rect(270, 310, 30, -120);
      $_p.rect(270, 190, 120, 30);
      $_p.rect(370, 190, 28, 210);
      $_p.rect(370, 370, 30, 30)
    }
    if (mapid === 1) {
      for (var x = -100; x < 400; x += 80) {
        for (var y = -100; y < 400; y += 80) {
        }
      }
      $_p.fill(0, 153, 255);
      $_p.rect(-20, 190, 90, 30, 80);
      $_p.rect(40, 190, 30, 130, 40);
      $_p.rect(40, 290, 420, 30, 40)
    }
  }
  
  function updateHp() {
    if (lives <= 0) {
      lost = 1
    }
  }
  
  function make_dart(x, y, r, s, range, cap, sd, nn) {
    if (nn === 500 && nn !== 1000 && nn !== -2.3 && nn !== -2.4) {
      dart.push({
        x: x,
        y: y,
        cap: 3,
        isNinja: 500,
        xs: $_p.cos(r) * s * 2,
        ys: $_p.sin(r) * s * 2,
        r: r,
        range: range,
        xx: x,
        yy: y
      })
    } else if (nn === 1000 && nn !== 500 && nn !== -2.3 && nn !== -2.4) {
      dart.push({
        x: x,
        y: y,
        cap: cap,
        isNinja: 1000,
        xs: $_p.cos(r) * s * 3,
        ys: $_p.sin(r) * s * 3,
        r: r,
        range: range,
        xx: x,
        yy: y
      })
    } else if (nn !== 1000 && nn !== 500 && nn !== -2.3 && nn !== -2.4) {
      if (sd > 1) {
        dart.push({
          x: x,
          y: y,
          cap: cap,
          xs: $_p.cos(r) * s * 2,
          ys: $_p.sin(r) * s * 2,
          r: r,
          range: range * 1,
          xx: x,
          yy: y
        })
      } else if (sd === 0) {
        dart.push({
          x: x + $_p.random(-14, 14),
          y: y + $_p.random(-14, 14),
          cap: cap,
          xs: $_p.cos(r) * s * 2,
          ys: $_p.sin(r) * s * 2,
          r: r,
          range: range * 20,
          xx: x,
          yy: y
        })
      } else if (nn === 22) {
        dart.push({
          x: x,
          y: y,
          cap: cap,
          isNinja: 22,
          xs: $_p.cos(r) * s * 3,
          ys: $_p.sin(r) * s * 3,
          r: r,
          range: range,
          xx: x,
          yy: y
        })
      } else if (nn === 99) {
        //playSound(getSound('retro/laser1'));
        dart.push({
          x: x,
          y: y,
          cap: cap,
          isNinja: 99,
          xs: $_p.cos(r) * s * 0.25,
          ys: $_p.sin(r) * s * 0.25,
          r: r,
          range: range,
          xx: x,
          yy: y
        })
      } else {
        dart.push({
          x: x,
          y: y,
          cap: cap,
          xs: $_p.cos(r) * s * 2,
          ys: $_p.sin(r) * s * 2,
          r: r,
          range: range,
          xx: x,
          yy: y
        })
      }
      return
    }
    if (nn === -2.3 || nn === -2.4) {
      dart.push({
        x: x,
        y: y,
        cap: cap,
        isNinja: nn,
        xs: $_p.cos(r) * s * 2,
        ys: $_p.sin(r) * s * 2,
        r: r,
        range: range,
        xx: x,
        yy: y
      })
    }
  }
  
  function update_monkey() {
    for (var i = 0; i < mon.length; i += 1) {
      mon[i].l++;
      if (mon[i].t === 3 || mon[i].t === 12) {
        mon[i].w += 2;
        if (mon[i].w > mon[i].d) {
          money += 50 * (mon[i].t - 2.5) * 2;
          mon[i].w = 0
        }
        banana_farm(mon[i].x, mon[i].y, mon[i].show)
      }
      var in_range = [];
      for (var www = 0; www < bal.length; www += 1) {
        if ($_p.dist(mon[i].x, mon[i].y, bal[www].x, bal[www].y) < mon[i].range / 2 || mon[i].t === 7 || mon[i].t === 11) {
          in_range.push(www)
        }
      }
      if (mon[i].t === 7) {
        mon[i].rotate = aimMouse(mon[i].x, mon[i].y) - 90
      } else if (mon[i].t === 11) {
        mon[i].rotate = aimMouse(mon[i].x, mon[i].y) / 2
      }
      $_p.translate(mon[i].x, mon[i].y);
      if (mon[i].t === 16 && mon[i].l % 20 === 0) {
        var temp = monkey_on;
        mon.push({
          x: $_p.random(mon[i].x - 40, mon[i].x + 40),
          y: $_p.random(mon[i].y - 40, mon[i].y + 40),
          t: 17,
          d: 10,
          cap: 2,
          l: 0,
          range: 200,
          w: 0,
          isNinja: 500
        });
        monkey_on = temp
      }
      $_p.rotate(mon[i].rotate + 180);
      if (mon[i].w >= mon[i].d && in_range.length) {
        switch (mon[i].t) {
        default: {
            mon[i].rotate = -$_p.atan2(bal[in_range[0]].x - mon[i].x, bal[in_range[0]].y - mon[i].y);
            make_dart(mon[i].x + $_p.cos(mon[i].rotate - 180) * 10, mon[i].y + $_p.sin(mon[i].rotate - 180) * 10, mon[i].rotate + 83, 10, mon[i].range * 2, mon[i].cap)
          }
          break;
        case 6: {
            mon[i].rotate = -$_p.atan2(bal[in_range[0]].x - mon[i].x, bal[in_range[0]].y - mon[i].y);
            make_dart(mon[i].x + $_p.cos(mon[i].rotate - 165) * 10, mon[i].y + $_p.sin(mon[i].rotate - 165) * 10, mon[i].rotate + 88, 10, mon[i].range * 2, mon[i].cap);
            make_dart(mon[i].x + $_p.cos(mon[i].rotate - 195) * 10, mon[i].y + $_p.sin(mon[i].rotate - 195) * 10, mon[i].rotate + 78, 10, mon[i].range * 2, mon[i].cap)
          }
          break;
        case 7: {
            make_dart(mon[i].x + $_p.cos(mon[i].rotate - 180) * 10, mon[i].y + $_p.sin(mon[i].rotate - 180) * 10, mon[i].rotate - 90, 10, 600, mon[i].cap, 0)
          }
          break;
        case 11: {
            mon[i].rotate = aimMouse(mon[i].x, mon[i].y);
            make_dart(mon[i].x + $_p.cos(mon[i].rotate - 180) * 10, mon[i].y + $_p.sin(mon[i].rotate - 180) * 10, mon[i].rotate - 180, 10, mon[i].range * 2, mon[i].cap, -999, 99)
          }
          break;
        case 14:
        case 15: {
            mon[i].rotate = -$_p.atan2(bal[in_range[0]].x - mon[i].x, bal[in_range[0]].y - mon[i].y);
            make_dart(mon[i].x + $_p.cos(mon[i].rotate - 180) * 10, mon[i].y + $_p.sin(mon[i].rotate - 180) * 10, mon[i].rotate + 83, 10, mon[i].range * 2, 1, 0, -2.3 - (mon[i].t - 14) * 0.1)
          }
          break;
        case 16: {
            mon[i].rotate = -$_p.atan2(bal[in_range[0]].x - mon[i].x, bal[in_range[0]].y - mon[i].y);
            make_dart(mon[i].x + $_p.cos(mon[i].rotate - 180) * 10, mon[i].y + $_p.sin(mon[i].rotate - 180) * 10, mon[i].rotate + 83, 10, mon[i].range * 2, mon[i].cap, 1, 500)
          }
          break;
        case 9:
        case 17: {
            mon[i].rotate = -$_p.atan2(bal[in_range[0]].x - mon[i].x, bal[in_range[0]].y - mon[i].y);
            make_dart(mon[i].x + $_p.cos(mon[i].rotate - 180) * 10, mon[i].y + $_p.sin(mon[i].rotate - 180) * 10, mon[i].rotate + 83, 10, mon[i].range * 2, mon[i].cap, 1, 500)
          }
          break;
        case 8:
        case 13: {
            mon[i].rotate = -$_p.atan2(bal[in_range[0]].x - mon[i].x, bal[in_range[0]].y - mon[i].y);
            make_dart(mon[i].x + $_p.cos(mon[i].rotate - 180) * 10, mon[i].y + $_p.sin(mon[i].rotate - 180) * 10, mon[i].rotate + 83, 10, mon[i].range * 2, mon[i].cap, 1, 1000)
          }
          break;
        case 10: {
            mon[i].rotate = -$_p.atan2(bal[in_range[0]].x - mon[i].x, bal[in_range[0]].y - mon[i].y);
            make_dart(mon[i].x + $_p.cos(mon[i].rotate - 180) * 10, mon[i].y + $_p.sin(mon[i].rotate - 180) * 10, mon[i].rotate + 83, 10, mon[i].range * 2, mon[i].cap, 1, 22)
          }
          break
        }
        mon[i].w = 0
      }
      if ($_p.dist(mon[i].x, mon[i].y, $_p.mouseX, $_p.mouseY) < 20 && $_p.mouseIsPressed && mon[i].t !== 17) {
        mon[i].show = true;
        monkey_is_pressed = true;
        monkey_on = i
      } else if ($_p.dist(mon[i].x, mon[i].y, $_p.mouseX, $_p.mouseY) > 20 && $_p.mouseX < 250 && $_p.mouseIsPressed && mon[i].t !== 17) {
        mon[i].show = false;
        if (monkey_on === i) {
          monkey_on = null;
          monkey_is_pressed = false
        }
      }
      switch (mon[i].t) {
      case 1:
        dart_monkey(0, 0, mon[i].show);
        break;
      case 2:
        super_monkey(0, 0, mon[i].show);
        break;
      case 3:
        banana_farm(0, 0, mon[i].show);
        break;
      case 4:
        spikes(0, 0, mon[i].show);
        break;
      case 5:
        dart_monkey10(0, 0, mon[i].show);
        break;
      case 6:
        super_monkey01(0, 0, mon[i].show);
        break;
      case 7:
        dartlingGun(0, 0, mon[i].show);
        break;
      case 8:
        ninja_monkey(0, 0, mon[i].show);
        break;
      case 9:
        engineer_monkey(0, 0, mon[i].show);
        break;
      case 10:
        spikePult(0, 0, mon[i].show);
        break;
      case 11:
        rodM(0, 0, mon[i].show, mon[i].rotate);
        break;
      case 12:
        factory(0, 0, mon[i].show, mon[i].rotate);
        break;
      case 13:
        ninja_monkey10(0, 0, mon[i].show, mon[i].rotate);
        break;
      case 14:
        cannon(0, 0, mon[i].show, mon[i].rotate);
        break;
      case 15:
        cannon(0, 0, mon[i].show, mon[i].rotate);
        break;
      case 16:
        sentry_monkey(0, 0, mon[i].show, mon[i].rotate);
        break;
      case 17:
        sentry(0, 0, mon[i].show, mon[i].rotate);
        break
      }
      if (mon[i].show === true) {
        $_p.strokeWeight(2);
        $_p.stroke(0, 0, 0);
        $_p.fill(0, 0, 0, 50);
        $_p.ellipse(0, 0, 40, 40)
      }
      $_p.noStroke();
      $_p.resetMatrix();
      mon[i].w += 1;
      if (mon[i].t === 17 && mon[i].l > 60) {
        mon.splice(i, 1)
      }
    }
  }
  
  function make_monkey(x, y, t) {
    switch (t) {
    case 1:
      mon.push({
        x: x,
        y: y,
        d: 30,
        l: 0,
        cap: 1,
        range: 100,
        rotate: 180,
        w: 0,
        superdartlings: 0,
        t: t,
        show: false,
        darts: 0
      });
      break;
    case 2:
      mon.push({
        x: x,
        y: y,
        cap: 1,
        l: 0,
        d: 1.2,
        range: 180,
        rotate: 180,
        w: 0,
        t: t,
        superdartlings: 0,
        show: false,
        darts: 0
      });
      break;
    case 3:
      mon.push({
        x: x,
        cap: 1,
        y: y,
        superdartlings: 0,
        l: 0,
        d: 300,
        range: 0,
        rotate: 180,
        w: 290,
        t: t,
        show: false,
        darts: 0
      });
      break;
    case 4:
      mon.push({
        x: x,
        y: y,
        d: 0.3,
        superdartlings: 0,
        l: 0,
        range: 35,
        cap: 2,
        rotate: 180,
        w: 0,
        t: t,
        spikes: 11,
        show: false,
        darts: 0
      });
      break;
    case 5:
      mon.push({
        x: x,
        l: 0,
        y: y,
        d: 24,
        superdartlings: 0,
        range: 160,
        rotate: 180,
        cap: 4,
        w: 0,
        t: t,
        show: false,
        darts: 0
      });
      break;
    case 6:
      mon.push({
        x: x,
        superdartlings: 0,
        y: y,
        d: 1.35,
        cap: 1,
        range: 320,
        rotate: 180,
        w: 0,
        t: t,
        show: false,
        darts: 0,
        l: 0
      });
      break;
    case 7:
      mon.push({
        x: x,
        y: y,
        l: 0,
        d: 1.4,
        cap: 1,
        range: 20,
        rotate: 180,
        w: 0,
        t: t,
        show: false,
        darts: 0,
        superdartlings: 1000
      });
      break;
    case 8:
      mon.push({
        x: x,
        y: y,
        l: 0,
        d: 10,
        cap: 2,
        range: 180,
        rotate: 180,
        w: 0,
        superdartlings: 0,
        isNinja: 1000,
        t: t,
        show: false,
        darts: 0
      });
      break;
    case 9:
      mon.push({
        x: x,
        y: y,
        d: 18,
        l: 0,
        cap: 3,
        range: 180,
        rotate: 180,
        w: 0,
        superdartlings: 0,
        isNinja: 500,
        t: t,
        show: false,
        darts: 0
      });
      break;
    case 10:
      mon.push({
        x: x,
        y: y,
        l: 0,
        d: 30,
        cap: 100,
        range: 360,
        rotate: 180,
        w: 0,
        superdartlings: -100,
        t: t,
        show: false,
        darts: 0
      });
      break;
    case 11:
      mon.push({
        x: x,
        y: y,
        d: 20,
        l: 0,
        cap: 200,
        range: 400,
        rotate: 180,
        w: 0,
        t: t,
        show: false,
        darts: 0
      });
      break;
    case 14:
      mon.push({
        x: x,
        y: y,
        d: 30,
        l: 0,
        rm: 1,
        dm: 1,
        cap: 1,
        range: 200,
        rotate: 180,
        w: 18,
        t: t,
        show: false,
        darts: 0
      });
      break
    }
  }
  
  function place_monkey() {
    var abc = false;
    if (place !== 0) {
      menu_hight = 0;
      monkey_is_pressed = false;
      switch (place) {
      case 1:
        var abc = true;
        for (var i = 0; i < mon.length; i++) {
          if ($_p.dist($_p.mouseX, $_p.mouseY, mon[i].x, mon[i].y) < 20) {
            abc = false
          }
        }
        if (abc) {
          dart_monkey($_p.mouseX, $_p.mouseY, true)
        }
        break;
      case 2:
        var abc = true;
        for (var i = 0; i < mon.length; i++) {
          if ($_p.dist($_p.mouseX, $_p.mouseY, mon[i].x, mon[i].y) < 20) {
            abc = false
          }
        }
        if (abc) {
          super_monkey($_p.mouseX, $_p.mouseY, true)
        }
        break;
      case 3:
        var abc = true;
        for (var i = 0; i < mon.length; i++) {
          if ($_p.dist($_p.mouseX, $_p.mouseY, mon[i].x, mon[i].y) < 20) {
            abc = false
          }
        }
        if (abc) {
          banana_farm($_p.mouseX, $_p.mouseY, true)
        }
        break;
      case 4:
        var abc = true;
        for (var i = 0; i < mon.length; i++) {
          if ($_p.dist($_p.mouseX, $_p.mouseY, mon[i].x, mon[i].y) < 20) {
            abc = false
          }
        }
        if (abc) {
          spikes($_p.mouseX, $_p.mouseY, true)
        }
        break;
      case 5:
        var abc = true;
        for (var i = 0; i < mon.length; i++) {
          if ($_p.dist($_p.mouseX, $_p.mouseY, mon[i].x, mon[i].y) < 20) {
            abc = false
          }
        }
        if (abc) {
          dart_monkey10($_p.mouseX, $_p.mouseY, true)
        }
        break;
      case 6:
        var abc = true;
        for (var i = 0; i < mon.length; i++) {
          if ($_p.dist($_p.mouseX, $_p.mouseY, mon[i].x, mon[i].y) < 20) {
            abc = false
          }
        }
        if (abc) {
          super_monkey01($_p.mouseX, $_p.mouseY, true)
        }
        break;
      case 7:
        var abc = true;
        for (var i = 0; i < mon.length; i++) {
          if ($_p.dist($_p.mouseX, $_p.mouseY, mon[i].x, mon[i].y) < 20) {
            abc = false
          }
        }
        if (abc) {
          dartlingGun($_p.mouseX, $_p.mouseY, true)
        }
        break;
      case 8:
        var abc = true;
        for (var i = 0; i < mon.length; i++) {
          if ($_p.dist($_p.mouseX, $_p.mouseY, mon[i].x, mon[i].y) < 20) {
            abc = false
          }
        }
        if (abc) {
          ninja_monkey($_p.mouseX, $_p.mouseY, true)
        }
        break;
      case 9:
        var abc = true;
        for (var i = 0; i < mon.length; i++) {
          if ($_p.dist($_p.mouseX, $_p.mouseY, mon[i].x, mon[i].y) < 20) {
            abc = false
          }
        }
        if (abc) {
          engineer_monkey($_p.mouseX, $_p.mouseY, true)
        }
        break;
      case 10:
        var abc = true;
        for (var i = 0; i < mon.length; i++) {
          if ($_p.dist($_p.mouseX, $_p.mouseY, mon[i].x, mon[i].y) < 20) {
            abc = false
          }
        }
        if (abc) {
          spikePult($_p.mouseX, $_p.mouseY, true)
        }
        break;
      case 11:
        var abc = true;
        for (var i = 0; i < mon.length; i++) {
          if ($_p.dist($_p.mouseX, $_p.mouseY, mon[i].x, mon[i].y) < 40) {
            abc = false
          }
        }
        if (abc) {
          $_p.stroke(0, 0, 0);
          $_p.strokeWeight(1);
          rodM($_p.mouseX, $_p.mouseY, true, 90)
        }
        break;
      case 14:
        var abc = true;
        for (var i = 0; i < mon.length; i++) {
          if ($_p.dist($_p.mouseX, $_p.mouseY, mon[i].x, mon[i].y) < 20) {
            abc = false
          }
        }
        if (abc) {
          $_p.stroke(0, 0, 0);
          $_p.strokeWeight(1);
          cannon($_p.mouseX, $_p.mouseY, true, 90)
        }
        break
      }
    }
    if (mouseTicks === 1 && place !== 0 && abc) {
      make_monkey($_p.mouseX, $_p.mouseY, place);
      place = 0
    }
  }
  
  function draw_menu(y) {
    $_p.fill(163, 163, 163);
    $_p.rect(10, 360 - y, 40, 40);
    $_p.fill(84, 84, 84);
    $_p.noStroke();
    $_p.triangle(20, 370 - y, 40, 370 - y, 30, 390 - y);
    $_p.stroke(0, 0, 0);
    $_p.strokeWeight(2);
    $_p.fill(163, 163, 163);
    $_p.rect(-10, 400 - y, 420, 100);
    if (tier === 0) {
      $_p.fill(117, 117, 117);
      if ($_p.dist($_p.mouseX, $_p.mouseY, 230, 425 - y) < 20 || code === 53) {
        $_p.fill(120, 120, 120, 145);
        $_p.strokeWeight(2);
        $_p.stroke(0, 0, 0);
        $_p.rect($_p.mouseX - 225, $_p.mouseY - 170, 300, 170, 20);
        $_p.fill(20, 18, 18, 200);
        $_p.textAlign($_p.CENTER);
        $_p.text('Spike-o-Pult,$' + prices.spikeopult + '.\n Fires massive spikey shots\n with 50 pierce each.\nHotkey: 5', $_p.mouseX - 75, $_p.mouseY - 150);
        $_p.fill(82, 82, 82);
        $_p.textAlign($_p.LEFT, $_p.CENTER);
        if ((code === 53 || mouseTicks === 1) && money >= prices.spikeopult && place < 1) {
          money -= prices.spikeopult;
          place = 10
        }
      } else {
        $_p.fill(117, 117, 117)
      }
      $_p.strokeWeight(1);
      $_p.stroke(0, 0, 0);
      $_p.strokeWeight(2);
      $_p.ellipse(230, 425 - y, 40, 40);
      $_p.push();
      $_p.scale(0.4, 0.4);
      $_p.translate(345, 640);
      spikePult(230, 441 - y * 2.5);
      $_p.pop();
      $_p.stroke(0, 0, 0);
      $_p.fill(117, 117, 117);
      if ($_p.dist($_p.mouseX, $_p.mouseY, 180, 425 - y) < 20 || code === 52) {
        $_p.fill(120, 120, 120, 145);
        $_p.rect($_p.mouseX, $_p.mouseY - 170, 300, 170, 20);
        $_p.fill(20, 18, 18, 200);
        $_p.textAlign($_p.CENTER);
        $_p.text('Monkey Engineer, $' + prices.engineer + '.\nUses his nail\ngun to create \n sentries, traps, and\nmore.\nHotkey: 4', $_p.mouseX + 150, $_p.mouseY - 150);
        $_p.fill(82, 82, 82);
        $_p.textAlign($_p.LEFT, $_p.CENTER);
        $_p.fill(82, 82, 82);
        if ((code === 52 || mouseTicks === 1) && money >= prices.engineer && place < 1) {
          money -= prices.engineer;
          place = 9
        }
      }
      $_p.ellipse(180, 425 - y, 40, 40);
      engineer_monkey(180, 421 - y);
      $_p.stroke(0, 0, 0);
      $_p.strokeWeight(2);
      $_p.fill(117, 117, 117);
      if ($_p.dist($_p.mouseX, $_p.mouseY, 30, 425 - y) < 20 || code === 49) {
        $_p.fill(120, 120, 120, 145);
        $_p.rect($_p.mouseX, $_p.mouseY - 170, 300, 170, 20);
        $_p.fill(20, 18, 18, 200);
        $_p.textAlign($_p.CENTER);
        $_p.text('Dart Monkey, $' + prices.dartm + '.\n Throws a single dart.\n Pops a single bloon.\nHotkey: 1', $_p.mouseX + 150, $_p.mouseY - 150);
        $_p.fill(82, 82, 82);
        $_p.textAlign($_p.LEFT, $_p.CENTER);
        $_p.fill(82, 82, 82);
        if ((mouseTicks === 1 || code === 49) && money >= prices.dartm && place < 1) {
          money += -150;
          place = 1
        }
      }
      $_p.ellipse(30, 425 - y, 40, 40);
      dart_monkey(30, 421 - y);
      $_p.stroke(0, 0, 0);
      $_p.strokeWeight(2);
      $_p.fill(117, 117, 117);
      if ($_p.dist($_p.mouseX, $_p.mouseY, 280, 425 - y) < 20 || code === 54) {
        $_p.fill(82, 82, 82);
        $_p.fill(120, 120, 120, 145);
        $_p.rect($_p.mouseX - 250, $_p.mouseY - 170, 280, 170, 20);
        $_p.fill(20, 18, 18, 200);
        $_p.textAlign($_p.CENTER);
        $_p.text('Ninja Monkey, $' + prices.ninja + '.\nStealthy tower throws\n shurikens rapidly.\n Pops 2 bloons each.\nHotkey: 6', $_p.mouseX - 110, $_p.mouseY - 150);
        $_p.textAlign($_p.LEFT, $_p.CENTER);
        $_p.fill(82, 82, 82);
        if ((code === 54 || mouseTicks === 1) && money >= prices.ninja && place < 1) {
          money -= prices.ninja;
          place = 8
        }
      }
      $_p.strokeWeight(2);
      $_p.ellipse(280, 425 - y, 40, 40);
      $_p.strokeWeight(1);
      ninja_monkey(280, 421 - y);
      $_p.stroke(0, 0, 0);
      $_p.strokeWeight(2);
      $_p.fill(117, 117, 117);
      if ($_p.dist($_p.mouseX, $_p.mouseY, 130, 425 - y) < 20 || code === 51) {
        $_p.fill(82, 82, 82);
        $_p.fill(120, 120, 120, 145);
        $_p.rect($_p.mouseX - 40, $_p.mouseY - 170, 300, 170, 20);
        $_p.fill(20, 18, 18, 200);
        $_p.textAlign($_p.CENTER);
        $_p.text('Banana Farm, $' + prices.bananafarm + '.\nDoes not attack. Generates cash.\n Good for long term investments.\nHotkey: 3', $_p.mouseX + 110, $_p.mouseY - 150);
        $_p.textAlign($_p.LEFT, $_p.CENTER);
        $_p.fill(82, 82, 82);
        if ((code === 51 || mouseTicks === 1) && money >= prices.bananafarm && place < 1) {
          money -= prices.bananafarm;
          place = 3
        }
      }
      $_p.stroke(0, 0, 0);
      $_p.strokeWeight(2);
      $_p.ellipse(130, 425 - y, 40, 40);
      banana_farm(130, 421 - y);
      $_p.strokeWeight(2);
      $_p.stroke(0, 0, 0);
      $_p.strokeWeight(2);
      $_p.fill(117, 117, 117);
      if ($_p.dist($_p.mouseX, $_p.mouseY, 80, 425 - y) < 20 || code === 50) {
        $_p.fill(120, 120, 120, 145);
        $_p.rect($_p.mouseX, $_p.mouseY - 170, 300, 170, 20);
        $_p.fill(20, 18, 18, 200);
        $_p.textAlign($_p.CENTER);
        $_p.text('Super Monkey, $' + prices.superm + '.\nThrows darts at a high rate. Great\n range. Each dart pops 1 bloon\nHotkey: 2', $_p.mouseX + 150, $_p.mouseY - 150);
        $_p.fill(82, 82, 82);
        $_p.textAlign($_p.LEFT, $_p.CENTER);
        if ((code === 50 || mouseTicks === 1) && money >= prices.superm && place < 1) {
          money -= 1000;
          place = 2
        }
      }
      $_p.ellipse(80, 425 - y, 40, 40);
      super_monkey(80, 421 - y);
      $_p.fill(117, 117, 117);
      if ($_p.dist($_p.mouseX, $_p.mouseY, 330, 425 - y) < 20 || code === 55) {
        $_p.strokeWeight(2);
        $_p.stroke(0, 0, 0);
        $_p.fill(82, 82, 82);
        $_p.fill(120, 120, 120, 145);
        $_p.rect($_p.mouseX - 300, $_p.mouseY - 170, 300, 170, 20);
        $_p.fill(20, 18, 18, 200);
        $_p.textAlign($_p.CENTER);
        $_p.text('Dartling Gun, $' + prices.dartlingm + '.\n Fires darts at mouse,\nrapidly but inaccurately.\nHotkey: 7', $_p.mouseX - 150, $_p.mouseY - 150);
        $_p.textAlign($_p.LEFT, $_p.CENTER);
        $_p.fill(82, 82, 82);
        if ((code === 55 || mouseTicks === 1) && money >= prices.dartlingm && place < 1) {
          money -= prices.dartlingm;
          place = 7
        }
        $_p.fill(82, 82, 82)
      }
      $_p.strokeWeight(2);
      $_p.stroke(0, 0, 0);
      $_p.ellipse(330, 425 - y, 40, 40);
      $_p.push();
      $_p.translate(330, 408 - y);
      $_p.scale(0.5);
      dartlingGun(0, 0);
      $_p.pop();
      $_p.fill(117, 117, 117);
      if ($_p.dist($_p.mouseX, $_p.mouseY, 380, 425 - y) < 20 || code === 56) {
        $_p.strokeWeight(2);
        $_p.stroke(0, 0, 0);
        $_p.fill(82, 82, 82);
        $_p.fill(120, 120, 120, 145);
        $_p.rect($_p.mouseX - 300, $_p.mouseY - 170, 300, 170, 20);
        $_p.fill(20, 18, 18, 200);
        $_p.textAlign($_p.CENTER);
        $_p.text('Bomb Tower, $' + prices.bomb + '.\n Fires a bomb, which explodes and\ndeals area damage to nearby bloons.\nHotkey: 8', $_p.mouseX - 150, $_p.mouseY - 150);
        $_p.textAlign($_p.LEFT, $_p.CENTER);
        $_p.fill(82, 82, 82);
        if ((code === 56 || mouseTicks === 1) && money >= prices.bomb && place < 1) {
          money -= prices.bomb;
          place = 14
        }
      }
      $_p.strokeWeight(2);
      $_p.stroke(0, 0, 0);
      $_p.ellipse(380, 425 - y, 40, 40);
      cannon(380, 421 - y);
      $_p.strokeWeight(1);
      $_p.strokeWeight(1)
    } else if (tier === 1) {
      $_p.fill(117, 117, 117);
      $_p.strokeWeight(2);
      if ($_p.dist($_p.mouseX, $_p.mouseY, 120, 425 - y) < 20) {
        $_p.fill(120, 120, 120, 145);
        $_p.rect($_p.mouseX, $_p.mouseY - 170, 300, 170, 20);
        $_p.fill(20, 18, 18, 200);
        $_p.textAlign($_p.CENTER);
        $_p.text('TestTower, $' + prices.x + '\nDescription', $_p.mouseX + 150, $_p.mouseY - 150);
        $_p.fill(82, 82, 82);
        $_p.textAlign($_p.LEFT, $_p.CENTER);
        if (mouseTicks === 1 && money >= prices.x) {
          money -= prices.x;
          place = 1
        }
      } else {
        $_p.fill(157, 157, 157)
      }
      $_p.stroke(0, 0, 0);
      $_p.strokeWeight(2);
      $_p.ellipse(130, 425 - y, 40, 40);
      $_p.push();
      $_p.push();
      factory(130, 430 - y, false, 90);
      $_p.pop()
    }
  }
  
  function menu_up() {
    if (menu_hight < 50) {
      menu_hight += 10
    }
  }
  
  function menu_down() {
    if (menu_hight > -2) {
      menu_hight -= 25
    }
  }
  
  function make_balloon(t) {
    if (t < 8) {
      bal.push({
        x: -100 + $_p.random(-10, 10),
        y: 200 + $_p.random(-4, 4),
        ph: 'left',
        speed: getSpeed(t),
        type: t
      })
    } else if (t === 8) {
      bal.push({
        x: -100 + $_p.random(-10, 10),
        y: 200 + $_p.random(-4, 4),
        ph: 'left',
        speed: getSpeed(t),
        type: t,
        hp: 9
      })
    } else {
      bal.push({
        x: -100 + $_p.random(-10, 10),
        y: 200 + $_p.random(-4, 4),
        ph: 'left',
        speed: getSpeed(t),
        type: t,
        hp: 75 * $_p.pow(1.025, $_p.max((frames - 4000) / 15, 0))
      })
    }
  }
  
  function update_dart() {
    for (var i = 0; i < dart.length; i += 1) {
      $_p.translate(dart[i].x, dart[i].y);
      $_p.rotate(dart[i].r + 90);
      draw_dart(0, 0, dart[i].isNinja);
      $_p.resetMatrix();
      dart[i].y += dart[i].ys;
      dart[i].x += dart[i].xs;
      if ($_p.dist(dart[i].x, dart[i].y, dart[i].xx, dart[i].yy) > dart[i].range / 1.5) {
        dart.splice(i, 1)
      }
    }
  }
  
  function upgrade(z) {
    money -= up[mon[z].t];
    switch (mon[z].t) {
    case 1:
      mon[z] = {
        x: mon[z].x,
        l: 15,
        superdartlings: 0,
        y: mon[z].y,
        d: 20,
        cap: 4,
        range: 180,
        rotate: 180,
        w: 19,
        t: 5,
        show: false,
        darts: 0
      };
      break;
    case 2:
      mon[z] = {
        x: mon[z].x,
        superdartlings: 0,
        y: mon[z].y,
        d: 1.35,
        cap: 2,
        l: 15,
        range: 200,
        rotate: 180,
        w: 0,
        t: 6,
        show: false,
        darts: 0
      };
      break;
    case 3:
      mon[z] = {
        x: mon[z].x,
        superdartlings: 0,
        y: mon[z].y,
        d: 120,
        l: 12,
        cap: 2,
        range: 0,
        rotate: 180,
        w: 75,
        t: 12,
        show: false,
        darts: 0
      };
      break;
    case 8:
      mon[z] = {
        x: mon[z].x,
        superdartlings: 0,
        y: mon[z].y,
        d: 8,
        l: 15,
        isNinja: 1000,
        cap: 3,
        range: 225,
        rotate: 180,
        w: 6,
        t: 13,
        show: false,
        darts: 0
      };
      break;
    case 9:
      mon[z] = {
        x: mon[z].x,
        y: mon[z].y,
        d: 18,
        l: 0,
        cap: 3,
        range: 180,
        rotate: 180,
        w: 0,
        superdartlings: 0,
        isNinja: 500,
        t: 16,
        show: false,
        darts: 0
      };
      break;
    case 14:
      mon[z] = {
        x: mon[z].x,
        superdartlings: 0,
        y: mon[z].y,
        d: 30,
        l: 15,
        isNinja: 0,
        cap: 1,
        range: 250,
        rotate: 180,
        w: 6,
        t: 15,
        show: false,
        darts: 0
      };
      break
    }
  }
  
  function mon_menu() {
    if (monkey_is_pressed) {
      if (mon[monkey_on].t === 17) {
        monkey_on--
      }
      $_p.fill(120, 120, 120);
      $_p.rect(250, 0, 150, 400);
      $_p.fill(26, 255, 0);
      if ($_p.mouseX > 270 && $_p.mouseX < 380 && $_p.mouseY > 50 && $_p.mouseY < 130 || code === 190) {
        $_p.fill(8, 150, 0);
        if ((code === 190 || mouseTicks === 1) && up[mon[monkey_on].t] > 0 && up[mon[monkey_on].t] <= money) {
          upgrade(monkey_on);
          mouseTicks = 0;
          monkey_is_pressed = false
        }
      }
      $_p.rect(270, 50, 110, 80);
      $_p.fill(255, 0, 0);
      $_p.rect(270, 150, 110, 80);
      $_p.textAlign($_p.CENTER);
      $_p.fill(0, 0, 0);
      $_p.text('SELL\n$' + sp[mon[monkey_on].t], 325, 180);
      $_p.text('$' + up[mon[monkey_on].t], 325, 80);
      $_p.text(name[mon[monkey_on].t], 325, 20);
      $_p.textSize(8);
      $_p.text('Hotkey: BACKSPACE', 325, 240);
      $_p.text(uname[mon[monkey_on].t], 325, 110);
      $_p.text('Hotkey: >', 325, 140);
      $_p.textAlign($_p.LEFT);
      $_p.textSize(20);
      if ($_p.mouseX > 270 && $_p.mouseX < 380 && $_p.mouseY > 150 && $_p.mouseY < 230 || code === 8) {
        $_p.fill(150, 8, 0);
        $_p.rect(270, 150, 110, 80);
        $_p.textAlign($_p.CENTER);
        $_p.fill(0, 0, 0);
        $_p.text('SELL\n$' + sp[mon[monkey_on].t], 325, 180);
        $_p.textSize(8);
        $_p.text('Hotkey: BACKSPACE', 325, 240);
        $_p.textAlign($_p.LEFT);
        if (mouseTicks === 1 && mon[monkey_on].l > 14 || code === 8) {
          sell(monkey_on);
          mouseTicks = 0;
          monkey_is_pressed = false
        }
      }
    }
  }
  
  function main_menu() {
    $_p.strokeWeight(2);
    $_p.fill(143, 143, 143);
    $_p.stroke(0, 0, 0);
    $_p.rect(0, -30, 399, 60, 10);
    $_p.textSize(20);
    $_p.fill(0, 0, 0);
    $_p.text('$' + $_p.ceil(money), 14, 20);
    $_p.textSize(20);
    $_p.fill(0, 0, 0);
    $_p.text('Bloons popped:' + bloonPop, 180, 20);
    $_p.text('Lives:' + $_p.ceil(lives), 80, 20)
  }
  
  function scroll() {
    if ($_p.mouseIsPressed && $_p.mouseX > 390 && mouseTicks === 4) {
      tier = 1;
      mouseTicks = 0
    }
    if ($_p.mouseIsPressed && $_p.mouseX < 10 && mouseTicks === 4) {
      tier = 0;
      mouseTicks = 0
    }
  }
  
  $_p.draw = function () {
    if (lost === 0) {
      frames++;
      scroll();
      updateHp();
      if ($_p.mouseIsPressed) {
        mouseTicks++
      } else {
        mouseTicks = 0
      }
      $_p.background(62, 117, 20);
      if (m > 15) {
        if (frames <= 650) {
          make_balloon(1)
        } else if (frames <= 1000) {
          make_balloon($_p.round($_p.random(0.5, 2.5)))
        } else if (frames <= 1400) {
          make_balloon($_p.round($_p.random(0.5, 3.5)))
        } else if (frames <= 1700) {
          make_balloon($_p.round($_p.random(0.5, 4.5)))
        } else if (frames <= 2200) {
          make_balloon($_p.round($_p.random(1.5, 5.5)))
        } else if (frames <= 3100) {
          make_balloon($_p.round($_p.random(5.5, 8.5)))
        } else if (frames <= 3300) {
          make_balloon(8);
          make_balloon(8)
        } else if (frames > 3300) {
          m2++;
          if (m2 > 9) {
            m2 = 0;
            make_balloon(9)
          }
        }
        m = 0
      }
      $_p.noStroke();
      track();
      update_balloons();
      update_dart();
      update_monkey();
      update_pop();
      update_expl();
      place_monkey();
      m += 1;
      draw_menu(menu_hight);
      main_menu();
      mon_menu();
      if ($_p.mouseY > 380 || menu_hight > 3) {
        menu_up()
      }
      if ($_p.mouseY < 320) {
        menu_down()
      }
    } else if (lost === 1) {
      $_p.textFont('Impact', 25);
      $_p.background(255, 0, 0);
      $_p.stroke(82, 2, 2);
      $_p.strokeWeight(2);
      $_p.textAlign($_p.CENTER);
      $_p.text('you losed', 200, 50)
    } else if (lost === 2) {
      track();
      $_p.fill(0, 0, 0);
      $_p.rect(200, 0, 5, 400);
      $_p.textSize(30);
      $_p.noStroke();
      $_p.text('map 1', 60, 60);
      $_p.text('map 2', 260, 60);
      $_p.text('press any $_p.keyto confirm', 45, 300);
      if ($_p.mouseIsPressed && $_p.mouseX < 200) {
        $_p.background(62, 117, 20);
        mapid = 0
      }
      if ($_p.mouseIsPressed && $_p.mouseX > 200) {
        $_p.background(0, 0, 0);
        mapid = 1
      }
      if ($_p.keyIsPressed) {
        lost = 0
      }
    }
  };
}
