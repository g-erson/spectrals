//enggine_development_example.scd
// thanks to @zebra: https://llllllll.co/t/norns-crone-supercollider/14616/129?u=mimetaur

// equivalently:
// Crone.setEngine('Engine_PolyPercPannable')

n = NetAddr.localAddr;
n.sendMsg('/engine/load/name', 'Spectrals');

n.sendMsg('/command/noteOn', 4); //pink noise or external
n.sendMsg('/command/noteOff', 4); //pink noise or external
n.sendMsg('/command/toggleInput'); //pink noise or external
n.sendMsg('/command/toggleEnv'); //envelopes on/off
//n.sendMsg('/command/toggleBank'); //edit A or B bank
n.sendMsg('/command/getBusAmp', 3);
n.sendMsg('/command/amp', 4, 1.8 );
n.sendMsg('/command/attack', 5);
n.sendMsg('/command/sustain', 1);
n.sendMsg('/command/release',0.3);
n.sendMsg('/command/ring', 2);
