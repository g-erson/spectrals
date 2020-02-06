//enggine_development_example.scd
// thanks to @zebra: https://llllllll.co/t/norns-crone-supercollider/14616/129?u=mimetaur

// equivalently:
// Crone.setEngine('Engine_PolyPercPannable')

(
n = NetAddr.localAddr;
n.sendMsg('/engine/load/name', 'Spectrals');
)

(
 16.do({arg item, i;
    n.sendMsg('/command/noteOn', i);
  });
);

(
  16.do({arg item, i;
    n.sendMsg('/command/noteOff', i);
  });
);

n.sendMsg('/command/dustOn'); //pink noise or external
n.sendMsg('/command/dustOff'); //pink noise or external
n.sendMsg('/command/noteOn', 3); //pink noise or external
n.sendMsg('/command/noteOff', 11); //pink noise or external
n.sendMsg('/command/toggleInput'); //pink noise or external
n.sendMsg('/command/toggleEnv'); //envelopes on/off
//n.sendMsg('/command/toggleBank'); //edit A or B bank
n.sendMsg('/command/getBusAmpControl');
n.sendMsg('/command/getBusAmpA', 3);
n.sendMsg('/command/getBusAmpB', 8);
n.sendMsg('/command/amp', 12, 0.01 );
n.sendMsg('/command/ampAll', 20 );
n.sendMsg('/command/attack', 0.2);
n.sendMsg('/command/sustain', 1);
n.sendMsg('/command/release',0.3);
n.sendMsg('/command/ring', 1);
