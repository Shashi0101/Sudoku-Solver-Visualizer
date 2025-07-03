import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Stack;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class SudokuSolverVisualizer extends JFrame {

    private static final int BOARD_SIZE = 9;
    private static final int CELL_SIZE = 60;
    private static final int FONT_SIZE = 30;

    private JTextField[][] cells = new JTextField[BOARD_SIZE][BOARD_SIZE];
    private JButton solveButton, resetButton, nextStepButton, autoSolveButton;
    private JLabel statusLabel;

    private int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
    private SwingWorker<Boolean, int[]> currentSolverWorker; // To manage the solving process in a background thread
    private boolean autoSolving = false;

    public SudokuSolverVisualizer() {
        setTitle("Sudoku Solver Visualizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel boardPanel = createBoardPanel();
        add(boardPanel, BorderLayout.CENTER);

        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);

        statusLabel = new JLabel("Enter Sudoku puzzle and click Solve.");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(statusLabel, BorderLayout.NORTH);

        pack();
        setLocationRelativeTo(null); // Center the window
        initializeBoard(); // Set up the initial board (can be empty or a predefined puzzle)
    }

    private JPanel createBoardPanel() {
        JPanel panel = new JPanel(new GridLayout(BOARD_SIZE, BOARD_SIZE));
        panel.setPreferredSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Padding around the grid

        Border cellBorder = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1);
        Border thickBorder = BorderFactory.createLineBorder(Color.BLACK, 2);

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                JTextField cell = new JTextField();
                cell.setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
                cell.setHorizontalAlignment(JTextField.CENTER);
                cell.setFont(new Font("Arial", Font.BOLD, FONT_SIZE));
                cell.setBorder(cellBorder);

                // Add thicker borders for 3x3 blocks
                if (row % 3 == 0 && row != 0) {
                    if (col % 3 == 0 && col != 0) {
                        cell.setBorder(BorderFactory.createMatteBorder(2, 2, 1, 1, Color.BLACK));
                    } else {
                        cell.setBorder(BorderFactory.createMatteBorder(2, 0, 1, 1, Color.BLACK));
                    }
                } else if (col % 3 == 0 && col != 0) {
                    cell.setBorder(BorderFactory.createMatteBorder(0, 2, 1, 1, Color.BLACK));
                }

                cells[row][col] = cell;
                panel.add(cell);
            }
        }
        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10)); // Added spacing

        solveButton = new JButton("Solve");
        solveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startSolving(false); // Start solving, not auto-solving initially
            }
        });

        nextStepButton = new JButton("Next Step");
        nextStepButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentSolverWorker != null && !currentSolverWorker.isDone()) {
                    synchronized (currentSolverWorker) {
                        currentSolverWorker.notify(); // Signal the worker to proceed
                    }
                }
            }
        });
        nextStepButton.setEnabled(false); // Disabled until a solve process starts

        autoSolveButton = new JButton("Auto Solve");
        autoSolveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startSolving(true); // Start auto-solving
            }
        });

        resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetBoard();
            }
        });

        panel.add(solveButton);
        panel.add(nextStepButton);
        panel.add(autoSolveButton);
        panel.add(resetButton);

        return panel;
    }

    private void initializeBoard() {
        // You can set an initial puzzle here, or leave it empty for user input
        // Example puzzle:
        int[][] initialPuzzle = {
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

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (initialPuzzle[r][c] != 0) {
                    cells[r][c].setText(String.valueOf(initialPuzzle[r][c]));
                    cells[r][c].setEditable(false); // Lock initial values
                    cells[r][c].setBackground(new Color(230, 230, 230)); // Grey out initial values
                } else {
                    cells[r][c].setText("");
                    cells[r][c].setEditable(true);
                    cells[r][c].setBackground(Color.WHITE);
                }
                board[r][c] = initialPuzzle[r][c]; // Initialize internal board as well
            }
        }
        statusLabel.setText("Puzzle loaded. Click Solve.");
    }

    private void resetBoard() {
        if (currentSolverWorker != null && !currentSolverWorker.isDone()) {
            currentSolverWorker.cancel(true); // Cancel any ongoing solving process
        }
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                cells[r][c].setText("");
                cells[r][c].setEditable(true);
                cells[r][c].setBackground(Color.WHITE);
                board[r][c] = 0; // Clear internal board
            }
        }
        solveButton.setEnabled(true);
        nextStepButton.setEnabled(false);
        autoSolveButton.setEnabled(true);
        statusLabel.setText("Board reset. Enter new puzzle or use example.");
        initializeBoard(); // Reload initial puzzle or clear
    }

    private void startSolving(boolean auto) {
        if (currentSolverWorker != null && !currentSolverWorker.isDone()) {
            currentSolverWorker.cancel(true); // Cancel any previous solving process
        }

        // Read the current board state from JTextFields
        if (!readBoardFromUI()) {
            statusLabel.setText("Invalid input! Please enter numbers 1-9 or leave empty.");
            return;
        }

        autoSolving = auto;
        solveButton.setEnabled(false);
        resetButton.setEnabled(false);
        autoSolveButton.setEnabled(false);
        nextStepButton.setEnabled(!autoSolving); // Enable next step only if not auto-solving

        statusLabel.setText("Solving...");

        currentSolverWorker = new SudokuSolverWorker(board);
        currentSolverWorker.execute(); // Start the SwingWorker
    }

    private boolean readBoardFromUI() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                String text = cells[r][c].getText().trim();
                if (text.isEmpty()) {
                    board[r][c] = 0;
                } else {
                    try {
                        int value = Integer.parseInt(text);
                        if (value >= 1 && value <= 9) {
                            board[r][c] = value;
                            if (!cells[r][c].isEditable()) { // If it was an initial value, keep it grey
                                cells[r][c].setBackground(new Color(230, 230, 230));
                            } else {
                                cells[r][c].setBackground(Color.WHITE);
                            }
                        } else {
                            cells[r][c].setBackground(Color.RED); // Highlight invalid input
                            return false;
                        }
                    } catch (NumberFormatException ex) {
                        cells[r][c].setBackground(Color.RED); // Highlight non-numeric input
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // Inner class for the Sudoku Solver using SwingWorker
    private class SudokuSolverWorker extends SwingWorker<Boolean, int[]> {

        private int[][] currentBoard; // A copy of the board to work on
        private final long stepDelayMs = 100; // Delay for auto-solve visualization
        private Stack<int[]> path; // Stores the path for backtracking visualization

        public SudokuSolverWorker(int[][] initialBoard) {
            this.currentBoard = new int[BOARD_SIZE][BOARD_SIZE];
            for (int r = 0; r < BOARD_SIZE; r++) {
                System.arraycopy(initialBoard[r], 0, this.currentBoard[r], 0, BOARD_SIZE);
            }
            this.path = new Stack<>();
        }

        @Override
        protected Boolean doInBackground() throws Exception {
            return solveSudoku(0, 0);
        }

        private boolean solveSudoku(int row, int col) throws InterruptedException {
            if (isCancelled()) return false;

            // Move to the next cell
            if (col == BOARD_SIZE) {
                col = 0;
                row++;
                if (row == BOARD_SIZE) {
                    // Board solved
                    publish(-1, -1, -1); // Signal solution found
                    return true;
                }
            }

            // If the current cell is not empty, move to the next
            if (currentBoard[row][col] != 0) {
                return solveSudoku(row, col + 1);
            }

            // Try placing numbers 1 to 9
            for (int num = 1; num <= 9; num++) {
                if (isSafe(row, col, num)) {
                    currentBoard[row][col] = num;
                    publish(row, col, num); // Publish the current state
                    path.push(new int[]{row, col, num}); // Push to path

                    if (!autoSolving) {
                        // Wait for "Next Step" button press if not auto-solving
                        synchronized (this) {
                            if (!isCancelled()) { // Check cancellation after publish and before wait
                                wait();
                            } else {
                                return false; // Exit if cancelled during wait
                            }
                        }
                    } else {
                        Thread.sleep(stepDelayMs); // Short delay for auto-solve
                    }

                    if (isCancelled()) return false;

                    if (solveSudoku(row, col + 1)) {
                        return true;
                    }

                    // Backtrack: If placing num didn't lead to a solution
                    currentBoard[row][col] = 0; // Reset the cell
                    publish(row, col, 0); // Publish the reset state
                    path.pop(); // Pop from path
                    if (!autoSolving) {
                        synchronized (this) {
                            if (!isCancelled()) {
                                wait(); // Wait after backtracking as well
                            } else {
                                return false;
                            }
                        }
                    } else {
                        Thread.sleep(stepDelayMs);
                    }
                    if (isCancelled()) return false;
                }
            }
            return false; // No number worked for this cell
        }

        private boolean isSafe(int row, int col, int num) {
            // Check row
            for (int x = 0; x < BOARD_SIZE; x++) {
                if (currentBoard[row][x] == num) {
                    return false;
                }
            }

            // Check column
            for (int x = 0; x < BOARD_SIZE; x++) {
                if (currentBoard[x][col] == num) {
                    return false;
                }
            }

            // Check 3x3 box
            int startRow = row - row % 3;
            int startCol = col - col % 3;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (currentBoard[i + startRow][j + startCol] == num) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        protected void process(java.util.List<int[]> chunks) {
            for (int[] update : chunks) {
                if (update[0] == -1) { // Signal for solution found
                    statusLabel.setText("Sudoku Solved!");
                    return;
                }
                int r = update[0];
                int c = update[1];
                int val = update[2];

                // Update UI based on the value
                if (val == 0) { // Backtracking, clear cell and change color
                    cells[r][c].setText("");
                    cells[r][c].setBackground(Color.RED); // Indicate backtracking
                } else {
                    cells[r][c].setText(String.valueOf(val));
                    cells[r][c].setBackground(Color.YELLOW); // Indicate new value tried
                }

                // Briefly show the color change, then revert to normal for next step
                Timer timer = new Timer(50, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (board[r][c] == 0) { // If it was an initially empty cell
                             cells[r][c].setBackground(Color.WHITE);
                        } else { // If it was an initial given number, keep it grey
                            cells[r][c].setBackground(new Color(230, 230, 230));
                        }
                        ((Timer)e.getSource()).stop(); // Stop the timer
                    }
                });
                timer.setRepeats(false);
                timer.start();
            }
        }

        @Override
        protected void done() {
            try {
                if (get()) { // True if the puzzle was solved
                    statusLabel.setText("Sudoku Solved Successfully!");
                    // Final update of the board if it's not perfectly synchronized
                    for (int r = 0; r < BOARD_SIZE; r++) {
                        for (int c = 0; c < BOARD_SIZE; c++) {
                            if (currentBoard[r][c] != 0) {
                                cells[r][c].setText(String.valueOf(currentBoard[r][c]));
                                if (board[r][c] == 0) { // If it was a filled cell by solver
                                     cells[r][c].setBackground(new Color(150, 255, 150)); // Green for solved cells
                                }
                            }
                        }
                    }
                } else {
                    statusLabel.setText("No solution found or process cancelled.");
                    for (int r = 0; r < BOARD_SIZE; r++) {
                        for (int c = 0; c < BOARD_SIZE; c++) {
                            if (board[r][c] == 0 && cells[r][c].isEditable()) { // Only reset cells that were initially empty and modified
                                cells[r][c].setText("");
                                cells[r][c].setBackground(Color.WHITE);
                            }
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                statusLabel.setText("Error during solving: " + e.getMessage());
                e.printStackTrace();
            } catch (CancellationException e) {
                statusLabel.setText("Solving cancelled.");
            } finally {
                solveButton.setEnabled(true);
                resetButton.setEnabled(true);
                autoSolveButton.setEnabled(true);
                nextStepButton.setEnabled(false);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SudokuSolverVisualizer().setVisible(true);
            }
        });
    }
}
