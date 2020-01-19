// CroneEngine_PolyPerc
// sine with perc envelopes, triggered on freq

// s = Server.local
// s.boot
//s.plotTree


/*
 Env Followers
 Scale selection as on SMR
 A/B Morphing as on 296e
 O->E and E->O and Rand Env follower assignment
 Odd/Even panned left/right
 'Rhythms' from clock


 [A] [B] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] 
 [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] 
 [ ] [ ] [X] [X] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] 
 [ ] [X] [X] [X] [X] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] 
 [X] [X] [X] [X] [X] [x] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] 
 [X] [X] [X] [X] [X] [x] [ ] [ ] [ ] [ ] [ ] [ ] [X] [ ] [ ] [ ] 
 [X] [X] [X] [X] [X] [x] [X] [ ] [ ] [ ] [ ] [X] [X] [ ] [ ] [ ] 
 [X] [X] [X] [X] [X] [x] [X] [ ] [ ] [ ] [X] [X] [X] [X] [ ] [ ] 
*/

Engine_Spectrals : CroneEngine {
  var pg;
  var amp=0.3;
  var ring=8; //specifies the Q resonance
  var attack=0.4;
  var sustain=1;
  var release=1;
  var toggleEnv=1;
  var toggleIn=1;
  var toggleBank=\A; // A or B

  // Arrays of filters
  var filterBankAList;
  var filterBankBList;
  var envList;    //shaping envelope
  var ampEnvList; //monitors level for each band
  var extOutList;

  var inBus; //Bus input for everything 
  var outBus; //16 bus out for everything 
  var noBus; //routed nowhere
  var envInBus; // optional envelope on spectral
  var envOutBus;

  var ampInBus; //amplitude of given spectral
  var ampOutBus; 

  var lfoBus;
  var fxBus;

  var barkScale;
  var currScale;
  var pinkNoise; //Pink Noise Input Synth
  var externalIn; //External Input Synth

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

  // Each filter should optionally have its own envelope
	alloc {
		pg = ParGroup.tail(context.xg);

    barkScale = Array[60, 150, 250, 350, 500, 630, 800, 1000, 1320, 1600, 2050, 2600, 3500, 5050, 8000, 12500];
    currScale = barkScale;

    filterBankAList    = List.new(16);
    filterBankBList    = List.new(16);
    envList            = List.new(16);
    ampEnvList         = List.new(16);
    extOutList         = List.new(16);

    inBus     = Bus.audio(context.server, 2);
    outBus    = currScale.collect{ Bus.audio(context.server) };

    ampInBus  = currScale.collect{ Bus.audio(context.server) };
    ampOutBus = currScale.collect{ Bus.control(context.server) };

    envInBus  = currScale.collect{ Bus.audio(context.server) };

    //Goes Nowhere
    noBus = Bus.audio(context.server,1);

    SynthDef(\Spectral, {
      arg in, out, amp_bus, freq = 440, ring=ring, amp;
      var inSignal = In.ar(in,2);

      var snd = DynKlank.ar(`[[freq], nil, [ring]], inSignal);
      var outSignal = (snd * amp);
      
      Out.ar(amp_bus, outSignal);
      Out.ar(out, outSignal);

    }).add;

    SynthDef(\SpectralEnv, {
      arg in, out, amp_bus, gate, attack=attack, sustain=sustain, release=release;
      var inSignal = In.ar(in);

      var outSignal = EnvGen.kr(Env.asr(attackTime: attack, releaseTime: release, sustainLevel: sustain), gate) * inSignal;

      Out.ar(amp_bus, outSignal);
      Out.ar(out, outSignal.dup);
    }).add;

    SynthDef(\NoiseIn, { 
      arg out;
      Out.ar(out,PinkNoise.ar(0.007, 0.007));
    }).add;

    // Reads stereo in to bus
    // toggleable
    SynthDef(\ExternalIn, { 
      arg out;
      Out.ar(out,Mix.new(SoundIn.ar([0, 1])));
    }).add;

    //reads N busses to stereo
    SynthDef(\ExternalOut, { 
      arg in, out, amp;
      Out.ar(out,In.ar(in).dup * amp);
    }).add;

    context.server.sync; // Wait for all the SynthDefs to be added on the server

		postln("Spectral: initPolls");

    // Default send pinkNoise
    pinkNoise = Synth(\NoiseIn, [
      \out, inBus
    ]);

    // Enable with toggle
    externalIn = Synth.after(pinkNoise, \ExternalIn, [
      \out, noBus
    ]);
    
    // for each frequency in scale
    barkScale.do({ arg freq, i;
      var filter, ampEnv, envelope, extOut;

      filter = Synth.after(externalIn, \Spectral, [
        \in, inBus,
        \out, envInBus[i],
        \amp_bus, noBus,
        \freq,freq,
        \ring, ring,
        \amp,0.3,
        \attack, attack,
        \release,release
      ]);

      envelope = Synth.after(filter, \SpectralEnv, [
        \in, envInBus[i],
        \out,  outBus[i],
        \amp_bus, ampInBus[i],
        \attack, attack,
        \sustain, sustain,
        \release, release,
        \gate, 0
      ]);

			ampEnv = Synth.after(envelope, \amp_env, [
				\in, ampInBus[i],
        \out, ampOutBus[i] 
      ]);

      extOut = Synth.after(envelope, \ExternalOut, [
        \in, outBus[i],
        \out, context.out_b,
        \amp, amp
      ]);

      filterBankAList.add(filter);
      ampEnvList.add(ampEnv);
      envList.add(envelope);
      extOutList.add(extOut);
    });

    ampOutBus.do({arg bus, i;
      context.registerPoll("band_amp_out_"++i, {bus.getSynchronous;});
    });

    this.addCommands;
  }

  addCommands {

    // id 
    this.addCommand("noteOn", "f", {
      arg msg;
      //Should only work with envelopes on!
      envList[msg[1]].set(\gate, 1);
    });

    //id
    this.addCommand("noteOff", "f", {
      arg msg;
      //Should only work with envelopes on!
      envList[msg[1]].set(\gate, 0);
    });

    //id of bus
    this.addCommand("getBusAmp", "f", {
      arg msg;
      postln("BUS: " + msg[1] + "VAL: " + ampOutBus[msg[1]].getSynchronous);
    });

		this.addCommand("toggleBank", "f", {
      postln("Current bank: " ++ toggleBank);
      if( toggleBank == \A, {
        toggleBank = \B;
      }, {
        toggleBank = \A;
      })
    });

    // Toggles Envelopes on each band on and off
		this.addCommand("toggleEnv", "f", {
      var outSignal;
      if( toggleEnv == 0, {
        filterBankAList.do({ arg filter, i; 
          filter.set(\out, envInBus[i]);
          filter.set(\amp_bus, noBus);
        });

        envList.do({arg env, i;
          env.set(\amp_bus, ampInBus[i]);
        });

        toggleEnv = 0;
      }, {
        filterBankAList.do({ arg filter, i; 
          filter.set(\out, outBus[i]);
          filter.set(\amp_bus, ampInBus[i]);
        });

        envList.do({arg env, i;
          env.set(\amp_bus, noBus);
        });
        toggleEnv = 1;
      })
		});

    // Toggles between pink noise and regular input
		this.addCommand("toggleInput", "f", {
      if( toggleIn == 1, {
        pinkNoise.set(\out, noBus);
        externalIn.set(\out,inBus);
        toggleIn = 0;
      }, {
        externalIn.set(\out,noBus);
        pinkNoise.set(\out, inBus);
        toggleIn = 1;
      })
		});

    // amp(id, amp)
		this.addCommand("amp", "f", { arg msg;
      var id;
      id = msg[1];
			amp = msg[2];
      
      filterBankAList[id].set(\amp, amp);
		});

		this.addCommand("ampAll", "f", { arg msg;
      var id;
      extOutList.do({arg item, i;
        item.set(\amp, msg[1]);
      });

		});

    // Q 
		this.addCommand("ring", "f", { arg msg;
			ring = msg[1];
      filterBankAList.do({ arg item, i; 
        item.set(\ring, ring);
      });
		});

		this.addCommand("attack", "f", { arg msg;
			attack = msg[1];
      envList.do({arg item, i;
        item.set(\attack, attack);
      });
		});

		this.addCommand("sustain", "f", { arg msg;
			sustain = msg[1];
      envList.do({arg item, i;
        item.set(\sustain, sustain);
      });
		});

		this.addCommand("release", "f", { arg msg;
			release = msg[1];
      envList.do({arg item, i;
        item.set(\release, release);
      });
		});
	}

	free {
    filterBankAList.do({arg item, i; item.free});
    envList.do({arg item, i; item.free});
    ampEnvList.do({arg item, i; item.free});
    extOutList.do({arg item, i; item.free});
	}
}
