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
  var amp=0.009; //Overall gain
  var ring=0.2; //specifies the Q resonance
  var attack=0.4;
  var sustain=1;
  var release=1;
  var toggleEnv=1;
  var toggleIn=1;
  var toggleBank=\A; // A or B

  // Arrays of filters
  var filterBankListA;
  var filterBankListB;
  var envListA;    //shaping envelope
  var envListB;    //shaping envelope
  var ampEnvListA; //monitors level for each band
  var ampEnvListB; //monitors level for each band
  var extOutListA;
  var extOutListB;

  var inBusA; //Bus input for everything 
  var inBusB; //Bus input for everything 

  // separate busses so we can split signal L+R to both, 
  // or L to one, R to other for spectral transfer

  var outBusA; //16 bus out for everything 
  var outBusB; //16 bus out for everything 
  // Separate busses to each ExternalOut synth for A/B morph

  var noBus; //routed nowhere
  var envInBusA; // optional envelope on spectral
  var envInBusB; // optional envelope on spectral
//  var envOutABus;

  var ampInBusA; //amplitude of given spectral
  var ampInBusB; //amplitude of given spectral

  var ampOutBusA; 
  var ampOutBusB; 

  var ampControlBus; //controls amplitude of spectral

  var lfoBus;
  var fxBus;

  var barkScale;
  var currScale;
  var pinkNoise; //Pink Noise Input Synth
  var dusty; //Dust Input Synth
  var externalIn; //External Input Synth

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

  // Each filter should optionally have its own envelope
	alloc {
		pg = ParGroup.tail(context.xg);

    barkScale = Array[90, 150, 240, 350, 500, 700, 840, 1000, 1170, 1370, 1600, 1850, 2150, 2900, 4000, 5800, 8500];
    currScale = barkScale;

    filterBankListA    = List.new(16);
    filterBankListB    = List.new(16);
    envListA            = List.new(16);
    envListB            = List.new(16);
    ampEnvListA         = List.new(16);
    ampEnvListB         = List.new(16);
    extOutListA         = List.new(16);
    extOutListB         = List.new(16);

    inBusA     = Bus.audio(context.server, 2);
    inBusB     = Bus.audio(context.server, 2);

    outBusA    = currScale.collect{ Bus.audio(context.server) };
    outBusB    = currScale.collect{ Bus.audio(context.server) };

    ampInBusA  = currScale.collect{ Bus.audio(context.server) };
    ampInBusB  = currScale.collect{ Bus.audio(context.server) };
    ampOutBusA = currScale.collect{ Bus.control(context.server) };
    ampOutBusB = currScale.collect{ Bus.control(context.server) };

    envInBusA  = currScale.collect{ Bus.audio(context.server) };
    envInBusB  = currScale.collect{ Bus.audio(context.server) };

    //Goes Nowhere
    noBus = Bus.audio(context.server,1);

    ampControlBus = Bus.control(context.server,1);
    ampControlBus.set(0.3);

    SynthDef(\Spectral, {
      arg in, out, amp_bus, freq = 440, ring=ring, amp;
      var inSignal = In.ar(in);

      var snd = DynKlank.ar(`[[freq], nil, [ring]], inSignal);
      var outSignal = (snd * In.kr(amp));
//      var outSignal = (snd * amp);
      
      Out.ar(amp_bus, outSignal);
      Out.ar(out, outSignal);

    }).add;

    SynthDef(\SpectralEnv, {
      arg in, out, amp_bus, gate, attack=attack, sustain=sustain, release=release, amp=1;
      var inSignal = In.ar(in);

      var outSignal = EnvGen.kr(Env.asr(
        attackTime: attack,
        releaseTime: release,
        sustainLevel: sustain
      ), gate) * inSignal * amp;

      Out.ar(amp_bus, outSignal);
      Out.ar(out, outSignal.dup);
    }).add;

    SynthDef(\NoiseIn, { 
      arg out;
      Out.ar(out,PinkNoise.ar(0.007, 0.007));
    }).add;

    SynthDef(\DustIn, { 
      arg out;
      Out.ar(out,Dust.ar(4, 0.5));
    }).add;

    // Reads stereo in to bus
    // toggleable
    SynthDef(\ExternalIn, { 
      arg out;
      Out.ar(out,Mix.new(SoundIn.ar([0, 1])));
    }).add;

    // Split stereo to different
    // busses
    SynthDef(\ExternalInSplit, { 
      arg out, out1;
      Out.ar(out,SoundIn.ar(0));
      Out.ar(out1,SoundIn.ar(1));
    }).add;

    //reads N busses to stereo
    SynthDef(\ExternalOut, { 
      arg in, out, amp;
      Out.ar(out,In.ar(in).dup * amp);
    }).add;

    context.server.sync; // Wait for all the SynthDefs to be added on the server

		postln("Spectral: initPolls");

    // Default send pinkNoise
    dusty = Synth(\DustIn, [
      \out, inBusB
    ]);

    pinkNoise = Synth.after(dusty, \NoiseIn, [
      \out, inBusA //noBus
    ]);

    // Enable with toggle
    externalIn = Synth.after(pinkNoise, \ExternalIn, [
      \out, noBus
    ]);

    // Set up Bank B first so it can modulate A
    barkScale.do({ arg freq, i;
      var filter, ampEnv, envelope, extOut;

      filter = Synth.after(externalIn, \Spectral, [
        \in, inBusB,
        \out, envInBusB[i],// context.out_b, //envInBusB[i],
        \amp_bus, noBus,
        \freq,freq,
        \ring, ring,
        \amp,ampControlBus,
      ]);

      envelope = Synth.after(filter, \SpectralEnv, [
        \in, envInBusB[i],
        \out,  outBusB[i],
        \amp_bus, ampInBusB[i],
        \attack, attack,
        \sustain, sustain,
        \release, release,
        \gate, 0
      ]);

			ampEnv = Synth.after(envelope, \amp_env, [
				\in, ampInBusB[i],
        \out, ampOutBusB[i] 
      ]);

      extOut = Synth.after(envelope, \ExternalOut, [
        \in, outBusB[i],
        \out, noBus, 
        \amp, amp
      ]);

      filterBankListB.add(filter);
      ampEnvListB.add(ampEnv);
      envListB.add(envelope);
      extOutListB.add(extOut);
    });
    
    // Set up Bank A
    barkScale.do({ arg freq, i;
      var filter, ampEnv, envelope, extOut;

      filter = Synth.after(extOutListB[extOutListB.size - 1], \Spectral, [
        \in, inBusA,
        \out, envInBusA[i],
        \amp_bus, noBus,
        \freq,freq,
        \ring, ring,
        \amp, ampOutBusB[i],
      ]);

      envelope = Synth.after(filter, \SpectralEnv, [
        \in, envInBusA[i],
        \out,  outBusA[i],
        \amp_bus, ampInBusA[i],
        \attack, attack,
        \sustain, sustain,
        \release, release,
        \gate, 0
      ]);

			ampEnv = Synth.after(envelope, \amp_env, [
				\in, ampInBusA[i],
        \out, ampOutBusA[i] 
      ]);

      extOut = Synth.after(envelope, \ExternalOut, [
        \in, outBusA[i],
        \out, context.out_b,
        \amp, amp 
      ]);

      filterBankListA.add(filter);
      ampEnvListA.add(ampEnv);
      envListA.add(envelope);
      extOutListA.add(extOut);
    });


    ampOutBusA.do({arg bus, i;
      context.registerPoll("band_amp_out_"++i, {bus.getSynchronous;});
    });

    this.addCommands;
  }

  addCommands {

    // id 
    this.addCommand("noteOn", "f", {
      arg msg;
      //Should only work with envelopes on!
      envListA[msg[1]].set(\gate, 1);
      envListB[msg[1]].set(\gate, 1);
    });

    //id
    this.addCommand("noteOff", "f", {
      arg msg;
      //Should only work with envelopes on!
      envListA[msg[1]].set(\gate, 0);
      envListB[msg[1]].set(\gate, 0);
    });

		this.addCommand("dustFreq", "f", { arg msg;
      dusty.set(\freq, msg[1]);
		});

		this.addCommand("dustOn", "f", { arg msg;
      dusty.set(\out, inBusB);
		});

		this.addCommand("dustOff", "f", { arg msg;
      dusty.set(\out, noBus);
		});

    //id of bus
    this.addCommand("getBusAmpControl", "f", {
      arg msg;
      postln("AMP CTRL BUS: " + ampControlBus + "VAL: " + ampControlBus.getSynchronous);
    });

    //id of bus
    this.addCommand("getBusAmpA", "f", {
      arg msg;
      postln("BUS A: " + msg[1] + "VAL: " + ampOutBusA[msg[1]].getSynchronous);
    });

    //id of bus
    this.addCommand("getBusAmpB", "f", {
      arg msg;
      postln("BUS B: " + msg[1] + "VAL: " + ampOutBusB[msg[1]].getSynchronous);
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
        filterBankListA.do({ arg filter, i; 
          filter.set(\out, envInBusA[i]);
          filter.set(\amp_bus, noBus);
        });

        envListA.do({arg env, i;
          env.set(\amp_bus, ampInBusA[i]);
        });

        toggleEnv = 0;
      }, {
        filterBankListA.do({ arg filter, i; 
          filter.set(\out, outBusA[i]);
          filter.set(\amp_bus, ampInBusA[i]);
        });

        envListA.do({arg env, i;
          env.set(\amp_bus, noBus);
        });
        toggleEnv = 1;
      })
		});

    // Toggles between pink noise and regular input
		this.addCommand("toggleInput", "f", {
      if( toggleIn == 1, {
        pinkNoise.set(\out, noBus);
        externalIn.set(\out,inBusA);
        toggleIn = 0;
      }, {
        externalIn.set(\out,noBus);
        pinkNoise.set(\out, inBusA);
        toggleIn = 1;
      })
		});

    // amp(id, amp)
		this.addCommand("amp", "f", { arg msg;
      var id;
      id = msg[1];
			amp = msg[2];
      
      filterBankListA[id].set(\amp, amp);
		});

		this.addCommand("ampAll", "f", { arg msg;
      var id;
      extOutListA.do({arg item, i;
        item.set(\amp, msg[1]);
      });
      extOutListB.do({arg item, i;
        item.set(\amp, msg[1]);
      });
		});

    // Q 
		this.addCommand("ring", "f", { arg msg;
			ring = msg[1];
      filterBankListA.do({ arg item, i; 
        item.set(\ring, ring);
      });
      filterBankListB.do({ arg item, i; 
        item.set(\ring, ring);
      });
		});

		this.addCommand("attack", "f", { arg msg;
			attack = msg[1];
      envListA.do({arg item, i;
        item.set(\attack, attack);
      });
		});

		this.addCommand("sustain", "f", { arg msg;
			sustain = msg[1];
      envListA.do({arg item, i;
        item.set(\sustain, sustain);
      });
		});

		this.addCommand("release", "f", { arg msg;
			release = msg[1];
      envListA.do({arg item, i;
        item.set(\release, release);
      });
		});
	}

	free {
    filterBankListA.do({arg item, i; item.free});
    envListA.do({arg item, i; item.free});
    ampEnvListA.do({arg item, i; item.free});
    extOutListA.do({arg item, i; item.free});
	}
}
