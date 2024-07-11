package visualizer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SudokuSolverVisualizer extends JPanel {
    private static final int SIZE = 9;
    private int[][] board;
    private boolean solved;
    private Timer timer;

    public SudokuSolverVisualizer(int[][] board) {
        this.board = board;
        this.solved = false;
        setPreferredSize(new Dimension(450, 450));
    }

    private boolean solveSudoku() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (board[row][col] == 0) {
                    for (int num = 1; num <= SIZE; num++) {
                        if (isSafe(row, col, num)) {
                            board[row][col] = num;
                            repaint();
                            try {
                                Thread.sleep(100); // To visualize the process
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                            if (solveSudoku()) {
                                return true;
                            }
                            board[row][col] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isSafe(int row, int col, int num) {
        for (int x = 0; x < SIZE; x++) {
            if (board[row][x] == num || board[x][col] == num || board[row - row % 3 + x / 3][col - col % 3 + x % 3] == num) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);

        for (int i = 0; i <= SIZE; i++) {
            g.drawLine(i * 50, 0, i * 50, 450);
            g.drawLine(0, i * 50, 450, i * 50);
        }

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (board[row][col] != 0) {
                    g.setFont(new Font("Arial", Font.BOLD, 20));
                    g.drawString(String.valueOf(board[row][col]), col * 50 + 20, row * 50 + 30);
                }
            }
        }
    }

    public static void main(String[] args) {
        int[][] board = {
            {5, 3, 0, 0, 7, 0, 0, 0, 0},
            {6, 0, 0, 1, 9, 5, 0, 0, 0},
            {0, 9, 8, 0, 0, 0, 0, 6, 0},
            {8, 0, 0, 0, 6, 0, 0, 0, 3},
            {4, 0, 0, 8, 0, 3, 0, 0, 1},
            {7, 0, 0, 0, 2, 0, 0, 0, 6},
            {0, 6, 0, 0, 0, 0, 2, 8, 0},
            {0, 0, 0, 4, 1, 9, 0, 0, 5},
            {0, 0, 0, 0, 8, 0, 0, 7, 9}
        };

        JFrame frame = new JFrame("Sudoku Solver Visualizer");
        SudokuSolverVisualizer visualizer = new SudokuSolverVisualizer(board);
        frame.add(visualizer, BorderLayout.CENTER);

        JButton solveButton = new JButton("Solve");
        solveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(() -> {
                    visualizer.solveSudoku();
                }).start();
            }
        });
        frame.add(solveButton, BorderLayout.SOUTH);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
