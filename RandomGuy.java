import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.lang.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.math.*;
import java.text.*;
import java.util.Arrays;

class RandomGuy {

    public Socket s;
	public BufferedReader sin;
	public PrintWriter sout;
    Random generator = new Random();

    double t1, t2;
    int me;
    int opp;
    int boardState;
    int state[][] = new int[8][8]; // state[0][0] is the bottom left corner of the board (on the GUI)
    int turn = -1;
    int round;
    int maxDepth = 5;
    long timeTaken = 0;
    boolean timeCrunch = false;
    
    int validMoves[] = new int[64];
    int numValidMoves;
    double[] firstRound;
    
    
    // main function that (1) establishes a connection with the server, and then plays whenever it is this player's turn
    public RandomGuy(int _me, String host) {
        me = _me;
        opp = me == 1 ? 2 : 1;

        initClient(host);

        int myMove;
        
        while (true) {
            System.out.println("Read");
            readMessage();
            
            if (turn == me) {
                long startOfTurn = System.currentTimeMillis();
                java.util.Timer t = new java.util.Timer();
                t.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (timeTaken + (System.currentTimeMillis() - startOfTurn) >= 170000) {
                            timeCrunch = true;
                        }
                    }
                }, 5000);
                System.out.println("Move");
                getValidMoves(round, state, me);
                
                myMove = move();
                
                String sel = validMoves[myMove] / 8 + "\n" + validMoves[myMove] % 8;
                
                System.out.println("Selection: " + validMoves[myMove] / 8 + ", " + validMoves[myMove] % 8);
                
                sout.println(sel);

                long endOfTurn = System.currentTimeMillis();
                timeTaken += (endOfTurn - startOfTurn);
            }
        }
    }
    
    // You should modify this function
    // validMoves is a list of valid locations that you could place your "stone" on this turn
    // Note that "state" is a global variable 2D list that shows the state of the game
    private int move() {
        // just move randomly for now
        int myMove = 0;
        if (round < 4 || timeCrunch) {
            myMove = generator.nextInt(numValidMoves);
        } else {
            firstRound = new double[numValidMoves];
            dfs(state, 0, Double.MIN_VALUE, Double.MAX_VALUE);
            myMove = findMove();
            getValidMoves(round, state, me);
            System.out.println("My Move is number: " + myMove);
        }
        
        return myMove;
    }

    private int findMove() {
        double best = Integer.MIN_VALUE;
        int move = 0;
        for (int i = 0; i < firstRound.length; i++) {
            if (firstRound[i] >= best) {
                best = firstRound[i];
                move = i;
            }
        }
        return move;
    }

    private double dfs(int[][] theState, int depth, double alpha, double beta) {
        if (depth < maxDepth) {
            int theTurn = depth % 2 == 0 ? me : opp;
            boolean max = theTurn == me;
            getValidMoves(round + depth, theState, theTurn);
            int length = numValidMoves;
            double theResult;
            double best;
            if (max) {
                best = Integer.MIN_VALUE;
                for (int i = 0; i < length; i++) {
                    int[][] currState = makeMove(theState, validMoves[i], theTurn);
                    theResult = dfs(currState, depth + 1, alpha, beta);
                    if (depth == 0) {
                        firstRound[i] = theResult;
                    }
                    best = max(best, theResult);
                    alpha = max(alpha, best);
                    if (beta <= alpha) {
                        break;
                    }
                    getValidMoves(round + depth, theState, theTurn);
                }
            } else {
                best = Integer.MAX_VALUE;                
                for (int i = 0; i < length; i++) {
                    int[][] currState = makeMove(theState, validMoves[i], theTurn);
                    best = min(best, dfs(currState, depth + 1, alpha, beta));
                    beta = min(beta, best);
                    if (beta <= alpha) {
                        break;
                    }
                    getValidMoves(round + depth, theState, theTurn);
                }
            }
            return best;
        }
        return getTotal(theState);
    }

    private double min(double first, double second) {
        if (first < second) {
            return first;
        }
        return second;
    }

    private double max(double first, double second) {
        if (first > second) {
            return first;
        }
        return second;
    }

    private double getTotal(int[][] theState) {
        double tilePlacement = tilePlacement(theState);
        double theCorners = corners(theState);
        double closeToCorners = adjacentToCorners(theState);
        double mobility = mobility(theState);
        return tilePlacement + (800 * theCorners) + (400 * closeToCorners) + (100 * mobility);
    }

    private double corners(int[][] theState) {
        int myCorners = 0;
        int oppCorners = 0;

        int bottomLeft = theState[0][0];
        int bottomRight = theState[0][7];
        int topLeft = theState[7][0];
        int topRight = theState[7][7];

        if (bottomLeft == me) {
            myCorners++;
        } else if (bottomLeft == opp) {
            oppCorners++;
        }

        if (bottomRight == me) {
            myCorners++;
        } else if (bottomRight == opp) {
            oppCorners++;
        }

        if (topLeft == me) {
            myCorners++;
        } else if (topLeft == opp) {
            oppCorners++;
        }
        
        if (topRight == me) {
            myCorners++;
        } else if (topRight == opp) {
            oppCorners++;
        }

        return 25 * (myCorners - oppCorners);
    }

    private double adjacentToCorners(int[][] theState) {
        int myAdj = 0;
        int oppAdj = 0;

        int bottomLeft = theState[0][0];
        int bottomRight = theState[0][7];
        int topLeft = theState[7][0];
        int topRight = theState[7][7];

        int above;
        int diag;
        int right;
        int left;
        int below;
        
        if (bottomLeft == 0) {
            above = theState[1][0];
            diag = theState[1][1];
            right = theState[0][1];
            if (above == me) {
                myAdj++;
            } else if (above == opp) {
                oppAdj++;
            }
            if (diag == me) {
                myAdj++;
            } else if (diag == opp) {
                oppAdj++;
            }
            if (right == me) {
                myAdj++;
            } else if (right == opp) {
                oppAdj++;
            }
        }

        if (bottomRight == 0) {
            above = theState[1][7];
            diag = theState[1][6];
            left = theState[0][6];
            if (above == me) {
                myAdj++;
            } else if (above == opp) {
                oppAdj++;
            }
            if (diag == me) {
                myAdj++;
            } else if (diag == opp) {
                oppAdj++;
            }
            if (left == me) {
                myAdj++;
            } else if (left == opp) {
                oppAdj++;
            }
        }

        if (topRight == 0) {
            below = theState[6][7];
            diag = theState[6][6];
            left = theState[7][6];
            if (below == me) {
                myAdj++;
            } else if (below == opp) {
                oppAdj++;
            }
            if (diag == me) {
                myAdj++;
            } else if (diag == opp) {
                oppAdj++;
            }
            if (left == me) {
                myAdj++;
            } else if (left == opp) {
                oppAdj++;
            }
        }

        if (topLeft == 0) {
            below = theState[6][0];
            diag = theState[6][1];
            right = theState[7][1];
            if (below == me) {
                myAdj++;
            } else if (below == opp) {
                oppAdj++;
            }
            if (diag == me) {
                myAdj++;
            } else if (diag == opp) {
                oppAdj++;
            }
            if (right == me) {
                myAdj++;
            } else if (right == opp) {
                oppAdj++;
            }
        }

        return 12.5 * (oppAdj - myAdj);
    }

    private double mobility(int[][] theState) {
        getValidMoves(5, theState, me);
        int myValidMoves = numValidMoves;
        getValidMoves(5, theState, opp);
        int oppValidMoves = numValidMoves;
        if(myValidMoves > oppValidMoves) {
            return 100 * myValidMoves / (double)(myValidMoves + oppValidMoves);
        } else if (myValidMoves < oppValidMoves) {
            return -100 * oppValidMoves / (double)(myValidMoves + oppValidMoves);
        }
        return 0;
    }

    private double tilePlacement(int[][] theState) {
        int myTiles = 0;
        int oppTiles = 0;
        int stable = 0;
        int stability[][] = { {20, -3, 11, 8, 8, 11, -3, 20},
                        {-3, -7, -4, 1, 1, -4, -7, -3},
                        {11, -4, 2, 2, 2, 2, -4, 11},
                        {8, 1, 2, -3, -3, 2, 1, 8},
                        {8, 1, 2, -3, -3, 2, 1, 8},
                        {11, -4, 2, 2, 2, 2, -4, 11},
                        {-3, -7, -4, 1, 1, -4, -7, -3},
                        {20, -3, 11, 8, 8, 11, -3, 20}  };
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (theState[i][j] == me) {
                    myTiles++;
                    stable += stability[i][j];
                } else if (theState[i][j] == opp) {
                    oppTiles++;
                    stable -= stability[i][j];
                }
            }
        }

        double tilesRatio = 0;
        if (myTiles > oppTiles) {
            tilesRatio = (100 * myTiles) / (double)(myTiles + oppTiles);
        } else if (myTiles < oppTiles) {
            tilesRatio = (-100 * oppTiles) / (double)(myTiles + oppTiles);
        }

        return 10 * (tilesRatio + stable);
    }

    private int[][] makeMove(int[][] currState, int moveNum, int myNum) {
        int[][] theState = new int[8][8];
        for (int i = 0; i < 8; i++) {
            theState[i] = Arrays.copyOf(currState[i], 8);
        }
        int moveX = moveNum / 8;
        int moveY = moveNum % 8;
        int currSquareX;
        int currSquareY;
        int theOpp = myNum == 1 ? 2 : 1;
        int maxLength = 7;
        int seqLength = 0;

        theState[moveX][moveY] = myNum;

        // Check below
        if (moveX > 1) {
            currSquareX = moveX - 1;
            while (currSquareX > 0 && theState[currSquareX][moveY] == theOpp) {
                currSquareX--;
                seqLength++;
            }
            if (theState[currSquareX][moveY] == myNum && seqLength > 0) {
                currSquareX = moveX - 1;
                while (seqLength > 0) {
                    theState[currSquareX][moveY] = myNum;
                    currSquareX--;
                    seqLength--;
                }
            }
        }
        // Check above
        if (moveX < maxLength - 1) {
            seqLength = 0;
            currSquareX = moveX + 1;
            while (currSquareX < maxLength && theState[currSquareX][moveY] == theOpp) {
                currSquareX++;
                seqLength++;
            }
            if (theState[currSquareX][moveY] == myNum && seqLength > 0) {
                currSquareX = moveX + 1;
                while (seqLength > 0) {
                    theState[currSquareX][moveY] = myNum;
                    currSquareX++;
                    seqLength--;
                }
            }
        }
        // Check left
        if (moveY > 1) {
            seqLength = 0;
            currSquareY = moveY - 1;
            while (currSquareY > 0 && theState[moveX][currSquareY] == theOpp) {
                currSquareY--;
                seqLength++;
            }
            if (theState[moveX][currSquareY] == myNum && seqLength > 0) {
                currSquareY = moveY - 1;
                while (seqLength > 0) {
                    theState[moveX][currSquareY] = myNum;
                    currSquareY--;
                    seqLength--;
                }
            }
        }
        // Check right
        if (moveY < maxLength - 1) {
            seqLength = 0;
            currSquareY = moveY + 1;
            while (currSquareY < maxLength && theState[moveX][currSquareY] == theOpp) {
                currSquareY++;
                seqLength++;
            }
            if (theState[moveX][currSquareY] == myNum && seqLength > 0) {
                currSquareY = moveY + 1;
                while (seqLength > 0) {
                    theState[moveX][currSquareY] = myNum;
                    currSquareY++;
                    seqLength--;
                }
            }
        }
        // Check diag-down-left
        if (moveX > 1 && moveY > 1) {
            seqLength = 0;
            currSquareX = moveX - 1;
            currSquareY = moveY - 1;
            while (currSquareX > 0 && currSquareY > 0 && theState[currSquareX][currSquareY] == theOpp) {
                currSquareX--;
                currSquareY--;
                seqLength++;
            }
            if (theState[currSquareX][currSquareY] == myNum && seqLength > 0) {
                currSquareX = moveX - 1;
                currSquareY = moveY - 1;
                while (seqLength > 0) {
                    theState[currSquareX][currSquareY] = myNum;
                    currSquareX--;
                    currSquareY--;
                    seqLength--;
                }
            }
        }
        // Check diag-down-right
        if (moveX > 1 && moveY < maxLength - 1) {
            seqLength = 0;
            currSquareX = moveX - 1;
            currSquareY = moveY + 1;
            while (currSquareX > 0 && currSquareY < maxLength && theState[currSquareX][currSquareY] == theOpp) {
                currSquareX--;
                currSquareY++;
                seqLength++;
            }
            if (theState[currSquareX][currSquareY] == myNum && seqLength > 0) {
                currSquareX = moveX - 1;
                currSquareY = moveY + 1;
                while (seqLength > 0) {
                    theState[currSquareX][currSquareY] = myNum;
                    currSquareX--;
                    currSquareY++;
                    seqLength--;
                }
            }
        }
        // Check diag-up-left
        if (moveX < maxLength - 1 && moveY > 1) {
            seqLength = 0;
            currSquareX = moveX + 1;
            currSquareY = moveY - 1;
            while (currSquareX < maxLength && currSquareY > 0 && theState[currSquareX][currSquareY] == theOpp) {
                currSquareX++;
                currSquareY--;
                seqLength++;
            }
            if (theState[currSquareX][currSquareY] == myNum && seqLength > 0) {
                currSquareX = moveX + 1;
                currSquareY = moveY - 1;
                while (seqLength > 0) {
                    theState[currSquareX][currSquareY] = myNum;
                    currSquareX++;
                    currSquareY--;
                    seqLength--;
                }
            }
        }
        // Check diag-up-right
        if (moveX < maxLength - 1 && moveY < maxLength - 1) {
            seqLength = 0;
            currSquareX = moveX + 1;
            currSquareY = moveY + 1;
            while (currSquareX < maxLength && currSquareY < maxLength && theState[currSquareX][currSquareY] == theOpp) {
                currSquareX++;
                currSquareY++;
                seqLength++;
            }
            if (theState[currSquareX][currSquareY] == myNum && seqLength > 0) {
                currSquareX = moveX + 1;
                currSquareY = moveY + 1;
                while (seqLength > 0) {
                    theState[currSquareX][currSquareY] = myNum;
                    currSquareX++;
                    currSquareY++;
                    seqLength--;
                }
            }
        }

        return theState;
    }
    
    // generates the set of valid moves for the player; returns a list of valid moves (validMoves)
    private void getValidMoves(int round, int state[][], int player) {
        int i, j;
        
        numValidMoves = 0;
        if (round < 4) {
            if (state[3][3] == 0) {
                validMoves[numValidMoves] = 3*8 + 3;
                numValidMoves ++;
            }
            if (state[3][4] == 0) {
                validMoves[numValidMoves] = 3*8 + 4;
                numValidMoves ++;
            }
            if (state[4][3] == 0) {
                validMoves[numValidMoves] = 4*8 + 3;
                numValidMoves ++;
            }
            if (state[4][4] == 0) {
                validMoves[numValidMoves] = 4*8 + 4;
                numValidMoves ++;
            }
            System.out.println("Valid Moves:");
            for (i = 0; i < numValidMoves; i++) {
                System.out.println(validMoves[i] / 8 + ", " + validMoves[i] % 8);
            }
        }
        else {
            System.out.println("Valid Moves:");
            for (i = 0; i < 8; i++) {
                for (j = 0; j < 8; j++) {
                    if (state[i][j] == 0) {
                        if (couldBe(state, i, j, player)) {
                            validMoves[numValidMoves] = i*8 + j;
                            numValidMoves ++;
                            System.out.println(i + ", " + j);
                        }
                    }
                }
            }
        }
    }
    
    private boolean checkDirection(int state[][], int row, int col, int incx, int incy, int player) {
        int sequence[] = new int[7];
        int seqLen;
        int i, r, c;
        
        seqLen = 0;
        for (i = 1; i < 8; i++) {
            r = row+incy*i;
            c = col+incx*i;
        
            if ((r < 0) || (r > 7) || (c < 0) || (c > 7))
                break;
        
            sequence[seqLen] = state[r][c];
            seqLen++;
        }
        
        int depth = 0;
        for (i = 0; i < seqLen; i++) {
            if (player == 1) {
                if (sequence[i] == 2)
                    depth ++;
                else {
                    if ((sequence[i] == 1) && (depth > 0))
                        return true;
                    break;
                }
            }
            else {
                if (sequence[i] == 1)
                    depth ++;
                else {
                    if ((sequence[i] == 2) && (depth > 0))
                        return true;
                    break;
                }
            }
        }
        
        return false;
    }
    
    private boolean couldBe(int state[][], int row, int col, int player) {
        int incx, incy;
        
        for (incx = -1; incx < 2; incx++) {
            for (incy = -1; incy < 2; incy++) {
                if ((incx == 0) && (incy == 0))
                    continue;
            
                if (checkDirection(state, row, col, incx, incy, player))
                    return true;
            }
        }
        
        return false;
    }
    
    public void readMessage() {
        int i, j;
        String status;
        try {
            //System.out.println("Ready to read again");
            turn = Integer.parseInt(sin.readLine());
            
            if (turn == -999) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
                
                System.exit(1);
            }
            
            //System.out.println("Turn: " + turn);
            round = Integer.parseInt(sin.readLine());
            t1 = Double.parseDouble(sin.readLine());
            System.out.println(t1);
            t2 = Double.parseDouble(sin.readLine());
            System.out.println(t2);
            for (i = 0; i < 8; i++) {
                for (j = 0; j < 8; j++) {
                    state[i][j] = Integer.parseInt(sin.readLine());
                }
            }
            sin.readLine();
        } catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
        }
        
        System.out.println("Turn: " + turn);
        System.out.println("Round: " + round);
        for (i = 7; i >= 0; i--) {
            for (j = 0; j < 8; j++) {
                System.out.print(state[i][j]);
            }
            System.out.println();
        }
        System.out.println();
    }
    
    public void initClient(String host) {
        int portNumber = 3333+me;
        
        try {
			s = new Socket(host, portNumber);
            sout = new PrintWriter(s.getOutputStream(), true);
			sin = new BufferedReader(new InputStreamReader(s.getInputStream()));
            
            String info = sin.readLine();
            System.out.println(info);
        } catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
        }
    }

    
    // compile on your machine: javac *.java
    // call: java RandomGuy [ipaddress] [player_number]
    //   ipaddress is the ipaddress on the computer the server was launched on.  Enter "localhost" if it is on the same computer
    //   player_number is 1 (for the black player) and 2 (for the white player)
    public static void main(String args[]) {
        new RandomGuy(Integer.parseInt(args[1]), args[0]);
    }
    
}
