module light1 (clk, rst, outN, outS, outE, outW, sensor_light, general_sensors, debug_port);
input clk, rst;

output [29:0]debug_port;

output [2:0]outN; /* For cars going north - idx 8 (see below) */
output [2:0]outS; /* For cars going south - idx 4 (see below) */
output [2:0]outE; /* For cars going east - idx 5 (see below) */
output [2:0]outW; /* For cars going west - idx 7 (see below) */

/* Assuming this is the light on the map, here is the index of the sensor in the light.  = 1 when a car is there	 

	 4
	 017
	523
	  6

	  */
input [7:0] sensor_light; 
input [29:0] general_sensors; /* a level has a max of 30 user placed sensors */

/* Stop = 3'b000, Forward_only = 3'b001, Left_only = 3'b010, Right_only = 3'b011, Go = 3'b100 */
parameter Stop = 3'b000,
	  Forward_only = 3'b001,
	  Left_only = 3'b010,
	  Right_only = 3'b011,
	  Go = 3'b100;

reg [7:0] count;
reg [2:0]outN;
reg [2:0]outS;
reg [2:0]outE;
reg [2:0]outW;

assign debug_port = count;

reg firstTime;

always @(posedge clk or negedge rst)
begin
	if (rst == 1'b0)
	begin
		count <= 8'd0;
		outN <= Go;
		outS <= Stop;
		outE <= Stop;
		outW <= Stop;
		firstTime <= 1'b0;
		count <= 8'd150;
	end
	else
	begin
		count <= count + 1'b1;
		if(count >= 8'd0 && count < 8'd210)
		begin
			outS <= Go;
			outN <= Stop;
			outW <= Stop;
			outE <= Stop;
		end
		else if(count >= 8'd210 && count < 8'd220)
		begin
			outS <= Stop;
			outN <= Stop;
			outW <= Go;
			outE <= Stop;
		end
		else if(count >= 8'd220 && count < 8'd230)
		begin
			outS <= Stop;
			outN <= Go;
			outW <= Stop;
			outE <= Stop;
		end
		else
		begin
			outS <= Stop;
			outN <= Stop;
			outW <= Stop;
			outE <= Go;
		end
		if(sensor_light[0] == 1'b1 || sensor_light[1] == 1'b1 || sensor_light[2] == 1'b1 || sensor_light[3] == 1'b1)
		begin
			outN <= Stop;
			outW <= Stop;
			outE <= Stop;
		end
	end
end

endmodule
