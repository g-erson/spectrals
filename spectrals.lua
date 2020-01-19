-- spectrals
-- 0.0.1 @ganders
-- llllllll.co/t/spectrals
--
--
-- E1/K2 : Change page
-- K3 : Change tab
-- E2/3 : Adjust parameters


local UI = require "ui"
-- local Graph = require "graph"
-- local EnvGraph = require "envgraph"

engine.name = "Spectrals"

-- engine.noteOn(4) //pink noise or external
-- engine.noteOff(4) //pink noise or external
-- engine.toggleInput() //pink noise or external
-- engine.toggleEnv() //envelopes on/off
-- engine.toggleBank() //edit A or B bank
-- engine.getBusAmp( 3) //prints debug
-- engine.amp(4, 1.8 ) //sets amp for spectral
-- engine.attack(5)
-- engine.sustain( 1)
-- engine.release(0.3)
-- engine.ring( 2)
 
-- Max amplitude of each band
amps = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }
ampLevels = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }
metroUpdate = {}
ampPolls = {}


function init()
  -- Sets a nice start 
  engine.noteOn(0)
  engine.noteOn(1)
  engine.noteOn(2)
  engine.noteOn(3)
  engine.noteOn(4)
  engine.noteOn(8)
  engine.noteOn(10)
  engine.noteOn(11)
  engine.noteOn(13)

  params:add_control("ring","ring",controlspec.new(0.01,10,'exp',0.02,0.6,''))
  params:set_action("ring", function(val) engine.ring(val) end)

  -- Everything Amp
  params:add_control("amp","amp",controlspec.new(0.1,9.99,'exp',0.1,0.2,''))
  params:set_action("amp", function(val) engine.ampAll(val*0.1) end)

--local vu_l, vu_r = poll.set("amp_in_l"), poll.set("amp_in_r")
--vu_l.time, vu_r.time = 1 / 30, 1 / 30
--vu_l.callback = function(val) in_l = val * 100 end
--vu_r.callback = function(val) in_r = val * 100 end
--vu_l:start()
--vu_r:start()
  for i = 1, 16 do
    ampPolls[i] = poll.set("band_amp_out_"..(i-1))
    ampPolls[i].time = 1 / 30
    ampPolls[i].callback = function(val) ampLevels[i] = val * 10 end
    ampPolls[i]:start()
  end

  metroUpdate = metro.init (function() redraw() end, 0.04) 
  metroUpdate:start()

  print("spectrals resonate")
end

-- Key input
function key(n, z)
  if z == 1 then
    
    if n == 2 then
      engine.toggleInput()

    elseif n == 3 then
      engine.toggleEnv()
      engine.noteOn(4)
      engine.noteOn(8)
      engine.noteOn(10)
      
    end
  end
end

function enc(n,d)
  -- encoder actions: n = number, d = delta
  if n == 1 then
  elseif n == 2 then
    params:delta("ring", d)
  else --n is 3
    params:delta("amp", d)
  end
  redraw()
end

function draw_amp_slider(x, y, index)
  screen.level(1)
  screen.move(x - 30, y - 17)
  screen.line(x - 30, y + 29)
  screen.stroke()
  screen.level(3)
  local r = util.clamp((ampLevels[index] * 100 or 0), 0, amps[index] * 34)
  screen.rect(x - 31.5, y + 29, 3, -r)
  screen.line_rel(3, 0)
  screen.fill()
  screen.level(6)
  screen.rect(x - 33, 54 - amps[index] / 3 * 114, 5, 2)
  screen.fill()
  screen.level(5)
--  screen.rect(x - 32, 49 - reel.rec.threshold / 15 * 10, 3, 1)
  screen.fill()
end

function redraw()
  screen.clear()
  screen.move(0,5)
  screen.level(3)
  screen.text("spectrals")
  screen.move(68,5)
  screen.level(9)
  screen.text("Q:" .. params:get("ring"))
  screen.move(95,5)
  screen.text("amp:" .. params:get("amp"))
--  screen.text("Q:" .. params:get("delaytime"))
--
--
  for i = 1, 16 do
    draw_amp_slider( 37 + (i*7), 30, i)
  end
  
  screen.update()
end

function cleanup()
  poll:clear_all()
  metro.free_all()
end

