import java.util.*;
import java.util.stream.Collectors;

public class TimeTableGenerator {

    // Constants
    private static final int POPULATION_SIZE = 100; // Increased population size for potentially harder problem
    private static final int GENERATIONS = 200;     // Increased generations
    private static final int PERIODS_PER_DAY = 8;
    private static final int DAYS_PER_WEEK = 5;
    private static final int TOTAL_PERIODS_PER_WEEK = PERIODS_PER_DAY * DAYS_PER_WEEK; // Should be 40
    private static final List<String> DAYS_OF_WEEK = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
    private static final double MUTATION_RATE = 0.15; // Slightly increased mutation rate

    // Max theory periods per subject per day (labs can exceed this)
    private static final int MAX_PERIODS_PER_THEORY_SUBJECT_PER_DAY = 2;

    // High penalty values for hard constraint violations
    private static final int HARD_CONSTRAINT_PENALTY = -500;

    private static final Random random = new Random();
    private static final Scanner scanner = new Scanner(System.in);

    static class Schedule {
        private final Map<String, List<String>> timetable;     // day -> list of subjects (No "FREE")
        private final Map<String, String> subjectStaffMap;     // subject -> staff
        private final Map<String, Boolean> isLabMap;           // subject -> is lab?
        private final Map<String, Integer> subjectsWithPeriods; // subject -> total weekly periods required
        private final Schedule otherSection;                   // used to penalize collisions
        private int fitness;

        Schedule(Map<String, List<String>> timetable,
                 Map<String, String> subjectStaffMap,
                 Map<String, Boolean> isLabMap,
                 Map<String, Integer> subjectsWithPeriods,
                 Schedule otherSection) {
            // Initialize with default values if null (though timetable should always be full now)
            this.timetable = (timetable != null) ? timetable : new HashMap<>();
            this.subjectStaffMap = (subjectStaffMap != null) ? subjectStaffMap : new HashMap<>();
            this.isLabMap = (isLabMap != null) ? isLabMap : new HashMap<>();
            this.subjectsWithPeriods = (subjectsWithPeriods != null) ? subjectsWithPeriods : new HashMap<>();
            this.otherSection = otherSection;
            // Ensure timetable structure is valid before calculating fitness
            if (isValidTimetableStructure(this.timetable)) {
                this.fitness = evaluateFitness();
            } else {
                this.fitness = Integer.MIN_VALUE; // Invalid structure gets worst possible fitness
            }
        }

        // Basic check if timetable has all days and correct number of periods per day
        private boolean isValidTimetableStructure(Map<String, List<String>> tt) {
            if (tt.size() != DAYS_PER_WEEK) return false;
            for (String day : DAYS_OF_WEEK) {
                if (!tt.containsKey(day) || tt.get(day).size() != PERIODS_PER_DAY) {
                    return false;
                }
                // Check if any period is null or accidentally "FREE"
                for(String period : tt.get(day)) {
                    if (period == null || period.equals("FREE")) return false;
                }
            }
            return true;
        }


        /**
         * Evaluates how "good" this schedule is based on constraints.
         * Higher score is better. Hard constraints have large penalties.
         */
        private int evaluateFitness() {
            int score = 0; // Start from 0, add penalties (negative values)
            Map<String, Integer> totalSubjectCount = new HashMap<>();

            // --- Hard Constraints ---

            // 1) Verify Correct Total Periods per Subject for the Week
            for (String day : DAYS_OF_WEEK) {
                List<String> daySchedule = timetable.get(day);
                for (String subject : daySchedule) {
                    totalSubjectCount.put(subject, totalSubjectCount.getOrDefault(subject, 0) + 1);
                }
            }
            for (Map.Entry<String, Integer> required : subjectsWithPeriods.entrySet()) {
                String subject = required.getKey();
                int requiredCount = required.getValue();
                int actualCount = totalSubjectCount.getOrDefault(subject, 0);
                if (actualCount != requiredCount) {
                    // Significant penalty if the total count for any subject is wrong
                    score += HARD_CONSTRAINT_PENALTY * Math.abs(actualCount - requiredCount);
                }
            }

            // 2) Lab Constraints: Must appear exactly once a week, in a consecutive block, matching required periods.
            for (Map.Entry<String, Integer> entry : subjectsWithPeriods.entrySet()) {
                String subject = entry.getKey();
                int requiredCount = entry.getValue();
                if (Boolean.TRUE.equals(isLabMap.get(subject))) {
                    List<String> daysWhereLabAppears = new ArrayList<>();
                    Map<String, List<Integer>> dayToIndices = new HashMap<>();
                    int actualTotalOccurrences = 0;

                    for (String day : DAYS_OF_WEEK) {
                        List<String> daySchedule = timetable.get(day);
                        for (int i = 0; i < daySchedule.size(); i++) {
                            if (subject.equals(daySchedule.get(i))) {
                                daysWhereLabAppears.add(day);
                                dayToIndices.computeIfAbsent(day, d -> new ArrayList<>()).add(i);
                                actualTotalOccurrences++;
                            }
                        }
                    }

                    // Penalty if total occurrences don't match required
                    if (actualTotalOccurrences != requiredCount) {
                        score += HARD_CONSTRAINT_PENALTY; // Penalty applied already in check #1, maybe redundant but reinforces
                    }

                    Set<String> uniqueDays = new HashSet<>(daysWhereLabAppears);

                    // Penalty if the lab appears on more than one day
                    if (uniqueDays.size() > 1) {
                        score += HARD_CONSTRAINT_PENALTY;
                        continue; // Skip further checks for this lab if it's already spread out
                    }

                    // Penalty if it appears 0 times (but required > 0) - covered by check #1

                    // Check for consecutiveness ONLY if it appears on exactly one day
                    if (uniqueDays.size() == 1) {
                        String day = uniqueDays.iterator().next();
                        List<Integer> indices = dayToIndices.get(day);
                        if (indices.size() != requiredCount) {
                             score += HARD_CONSTRAINT_PENALTY; // Count mismatch on the day
                        } else {
                            Collections.sort(indices);
                            boolean consecutive = true;
                            for (int k = 0; k < indices.size() - 1; k++) {
                                if (indices.get(k + 1) != indices.get(k) + 1) {
                                    consecutive = false;
                                    break;
                                }
                            }
                            if (!consecutive) {
                                score += HARD_CONSTRAINT_PENALTY; // Penalty for non-consecutive indices
                            }
                        }
                    } else if (requiredCount > 0 && uniqueDays.isEmpty()) {
                         // If it was required but didn't appear at all (Should be caught by #1)
                         score += HARD_CONSTRAINT_PENALTY;
                    }
                }
            }

            // 3) Staff Collision Penalty (if otherSection exists)
            if (otherSection != null && isValidTimetableStructure(otherSection.timetable)) {
                Map<String, List<String>> otherTimetable = otherSection.getTimetable();
                for (String day : DAYS_OF_WEEK) {
                    List<String> dayScheduleThis = timetable.get(day);
                    List<String> dayScheduleOther = otherTimetable.get(day); // Assume other is valid structure too
                     if (dayScheduleOther == null) continue; // Should not happen if valid

                    for (int i = 0; i < PERIODS_PER_DAY; i++) {
                        String subjThis = dayScheduleThis.get(i);
                        String subjOther = dayScheduleOther.get(i);
                        // No need to check for "FREE"
                        String staffThis = subjectStaffMap.get(subjThis);
                        String staffOther = otherSection.subjectStaffMap.get(subjOther);

                        if (staffThis != null && staffThis.equals(staffOther)) {
                            score += HARD_CONSTRAINT_PENALTY; // Large penalty for staff clash
                        }
                    }
                }
            }

            // --- Soft Constraints ---

            // 4) Penalize consecutive same *theory* subject in a day
            for (String day : DAYS_OF_WEEK) {
                List<String> daySchedule = timetable.get(day);
                for (int i = 0; i < daySchedule.size() - 1; i++) {
                    String subj = daySchedule.get(i);
                    if (subj.equals(daySchedule.get(i + 1))) {
                        // Penalize only if it's a theory subject repeated
                        if (!isLabMap.getOrDefault(subj, false)) {
                            score -= 5; // Smaller penalty for soft constraint
                        }
                    }
                }
            }

            // 5) Penalize if a *theory* subject appears too many times (> MAX_PERIODS...) in one day
            for (String day : DAYS_OF_WEEK) {
                List<String> daySchedule = timetable.get(day);
                Map<String, Integer> dailySubjectCount = new HashMap<>();
                for (String subject : daySchedule) {
                    dailySubjectCount.put(subject, dailySubjectCount.getOrDefault(subject, 0) + 1);
                }
                for (Map.Entry<String, Integer> entry : dailySubjectCount.entrySet()) {
                    String subject = entry.getKey();
                    int count = entry.getValue();
                    // Check only theory subjects
                    if (!isLabMap.getOrDefault(subject, false)) {
                        if (count > MAX_PERIODS_PER_THEORY_SUBJECT_PER_DAY) {
                            score -= 10 * (count - MAX_PERIODS_PER_THEORY_SUBJECT_PER_DAY); // Penalty increases with excess
                        }
                    }
                }
            }

            // Maybe add a small bonus for spreading subjects out? (Optional)
            // Example: Small bonus for each day that has more unique subjects
             for (String day : DAYS_OF_WEEK) {
                 Set<String> uniqueSubjectsToday = new HashSet<>(timetable.get(day));
                 score += uniqueSubjectsToday.size(); // Small bonus per unique subject on a day
             }


            return score;
        }

        public Map<String, List<String>> getTimetable() {
            return timetable;
        }

        public int getFitness() {
            return fitness;
        }

        // Call this after mutation
        public void recalcFitness() {
             if (isValidTimetableStructure(this.timetable)) {
                 this.fitness = evaluateFitness();
             } else {
                 this.fitness = Integer.MIN_VALUE;
             }
        }
    }

    /**
     * Generates a random valid schedule for one section, attempting to place labs correctly first.
     * Assumes total periods = TOTAL_PERIODS_PER_WEEK.
     */
    static Schedule generateRandomSchedule(Map<String, Integer> subjectsWithPeriods,
                                           Map<String, String> subjectStaffMap,
                                           Map<String, Boolean> isLabMap,
                                           Schedule otherSection) {
        Map<String, List<String>> timetable = new HashMap<>();
        // Initialize timetable with nulls or a placeholder initially
        for (String day : DAYS_OF_WEEK) {
            timetable.put(day, new ArrayList<>(Collections.nCopies(PERIODS_PER_DAY, null))); // Use null as placeholder
        }

        List<String> labs = subjectsWithPeriods.entrySet().stream()
                .filter(e -> isLabMap.getOrDefault(e.getKey(), false))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<String> theories = subjectsWithPeriods.entrySet().stream()
                .filter(e -> !isLabMap.getOrDefault(e.getKey(), false))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        boolean labsPlacedSuccessfully = placeLabsRandomly(timetable, labs, subjectsWithPeriods);

        if (!labsPlacedSuccessfully) {
             System.err.println("Warning: Could not place all labs correctly in initial random schedule. Fitness will be very low.");
             // Fallback: Just fill everything randomly (will likely violate constraints)
             fillRemainingSlotsRandomly(timetable, subjectsWithPeriods);
        } else {
            // Labs are placed, now fill remaining slots with theories
            fillRemainingSlotsWithTheories(timetable, theories, subjectsWithPeriods);
        }

        // Final check: Ensure no nulls remain (should not happen if logic is correct and total periods = 40)
        for(List<String> daySchedule : timetable.values()){
            if(daySchedule.contains(null)){
                 System.err.println("Error: Null period found after generation. Filling remaining with first theory.");
                 String firstTheory = theories.isEmpty() ? subjectsWithPeriods.keySet().iterator().next() : theories.get(0); // Failsafe subject
                 for(int i=0; i<daySchedule.size(); i++){
                     if(daySchedule.get(i) == null){
                         daySchedule.set(i, firstTheory);
                     }
                 }
            }
        }


        return new Schedule(timetable, subjectStaffMap, isLabMap, subjectsWithPeriods, otherSection);
    }

    // Attempts to place all labs in valid consecutive slots
    private static boolean placeLabsRandomly(Map<String, List<String>> timetable,
                                            List<String> labs,
                                            Map<String, Integer> subjectsWithPeriods) {
        Collections.shuffle(labs); // Randomize lab placement order

        for (String lab : labs) {
            int duration = subjectsWithPeriods.get(lab);
            boolean placed = false;
            int attempts = 0;
            final int maxAttempts = 100; // Prevent infinite loops

            while (!placed && attempts < maxAttempts) {
                attempts++;
                String randomDay = DAYS_OF_WEEK.get(random.nextInt(DAYS_PER_WEEK));
                int randomStartPeriod = random.nextInt(PERIODS_PER_DAY - duration + 1); // Valid start index

                // Check if the block is free
                boolean blockIsFree = true;
                for (int i = 0; i < duration; i++) {
                    if (timetable.get(randomDay).get(randomStartPeriod + i) != null) {
                        blockIsFree = false;
                        break;
                    }
                }

                // If free, place the lab block
                if (blockIsFree) {
                    for (int i = 0; i < duration; i++) {
                        timetable.get(randomDay).set(randomStartPeriod + i, lab);
                    }
                    placed = true;
                }
            }
            if (!placed) {
                System.err.println("Failed to place lab: " + lab + " after " + maxAttempts + " attempts.");
                return false; // Failed to place this lab
            }
        }
        return true; // All labs placed successfully
    }

    // Fills remaining null slots with theories based on their required counts
    private static void fillRemainingSlotsWithTheories(Map<String, List<String>> timetable,
                                                      List<String> theories,
                                                      Map<String, Integer> subjectsWithPeriods) {
        // Create a pool of theory subjects based on their required counts
        List<String> theoryPool = new ArrayList<>();
        for (String theory : theories) {
            theoryPool.addAll(Collections.nCopies(subjectsWithPeriods.get(theory), theory));
        }
        Collections.shuffle(theoryPool);

        // Iterate through all slots and fill nulls from the pool
        for (String day : DAYS_OF_WEEK) {
            List<String> daySchedule = timetable.get(day);
            for (int i = 0; i < PERIODS_PER_DAY; i++) {
                if (daySchedule.get(i) == null) { // If slot is empty
                    if (!theoryPool.isEmpty()) {
                        daySchedule.set(i, theoryPool.remove(0)); // Take next from shuffled pool
                    } else {
                        // Should not happen if total periods = 40 and labs placed correctly
                        System.err.println("Error: Theory pool empty but null slots remain on " + day + " period " + (i+1));
                        // Failsafe: Put a placeholder or first theory again
                         String failsafeSubject = theories.isEmpty() ? subjectsWithPeriods.keySet().stream().filter(s -> !subjectsWithPeriods.get(s).equals(true)).findFirst().orElse("THEORY_ERR") : theories.get(0);
                         daySchedule.set(i, failsafeSubject);
                    }
                }
            }
        }
         if (!theoryPool.isEmpty()) {
              System.err.println("Warning: Theory pool not empty after filling all slots. Count mismatch likely.");
         }
    }

    // Fallback method if lab placement fails - just fills everything randomly
     private static void fillRemainingSlotsRandomly(Map<String, List<String>> timetable,
                                                   Map<String, Integer> subjectsWithPeriods) {
         List<String> subjectPool = new ArrayList<>();
         for (Map.Entry<String, Integer> entry : subjectsWithPeriods.entrySet()) {
             subjectPool.addAll(Collections.nCopies(entry.getValue(), entry.getKey()));
         }
         Collections.shuffle(subjectPool);

         int poolIndex = 0;
         for (String day : DAYS_OF_WEEK) {
             List<String> daySchedule = timetable.get(day);
             for (int i = 0; i < PERIODS_PER_DAY; i++) {
                 // Overwrite regardless of what's there in this fallback
                 if (poolIndex < subjectPool.size()) {
                     daySchedule.set(i, subjectPool.get(poolIndex++));
                 } else {
                     // Should not happen if counts are correct
                     System.err.println("Error: Subject pool exhausted prematurely in fallback fill.");
                     daySchedule.set(i, "FILL_ERR");
                 }
             }
         }
     }


    /**
     * Run the Genetic Algorithm to evolve a schedule.
     */
    static Schedule runGeneticAlgorithm(Map<String, Integer> subjectsWithPeriods,
                                        Map<String, String> subjectStaffMap,
                                        Map<String, Boolean> isLabMap,
                                        Schedule otherSection) {
        if (subjectsWithPeriods.isEmpty()) {
            throw new IllegalArgumentException("No subjects provided.");
        }
        // Validation already done in main for total periods == 40

        // 1) Initialize population
        List<Schedule> population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population.add(generateRandomSchedule(subjectsWithPeriods, subjectStaffMap, isLabMap, otherSection));
        }

        Schedule bestScheduleOverall = null;
        int bestFitnessOverall = Integer.MIN_VALUE;

        // 2) Evolve
        for (int generation = 0; generation < GENERATIONS; generation++) {
             // Calculate fitness for any schedule that might have been mutated
             // Note: Constructor calculates initial fitness. Recalc needed after mutation.
             // (Mutation calls recalcFitness itself)

            // Sort by descending fitness
            population.sort(Comparator.comparingInt(Schedule::getFitness).reversed());

            Schedule currentBest = population.get(0);
            if (currentBest.getFitness() > bestFitnessOverall) {
                bestFitnessOverall = currentBest.getFitness();
                bestScheduleOverall = currentBest; // Keep track of the best *ever* seen
                 System.out.println("Generation " + generation + " - New Best Fitness: " + bestFitnessOverall);
            } else if (generation % 20 == 0) { // Print status less frequently
                 System.out.println("Generation " + generation + " - Current Best: " + population.get(0).getFitness() + " (Overall Best: " + bestFitnessOverall + ")");
            }


            // Selection: Elitism (keep best 10%) + Tournament/Random selection for breeding
            List<Schedule> newPopulation = new ArrayList<>();
            int eliteSize = Math.max(1, POPULATION_SIZE / 10); // Keep at least 1 elite
            newPopulation.addAll(population.subList(0, eliteSize));

            // Breed remaining population
            while (newPopulation.size() < POPULATION_SIZE) {
                 // Select two parents (e.g., randomly from the top 50% or tournament)
                 Schedule parent1 = selectParentTournament(population);
                 Schedule parent2 = selectParentTournament(population);

                // Ensure parents are different for crossover
                int attempts = 0;
                while (parent1 == parent2 && population.size() > 1 && attempts < 10) {
                    parent2 = selectParentTournament(population);
                    attempts++;
                }

                Schedule child = crossover(parent1, parent2, subjectStaffMap, isLabMap, subjectsWithPeriods, otherSection);

                if (random.nextDouble() < MUTATION_RATE) {
                    mutate(child); // Mutate the child
                } else {
                    // If no mutation, fitness is already calculated by constructor.
                    // If mutation happened, recalcFitness was called inside mutate().
                }
                newPopulation.add(child);
            }
            population = newPopulation;
        }

        // Return best overall schedule found during the run
        System.out.println("Finished GA. Best fitness achieved: " + bestFitnessOverall);
        if (bestScheduleOverall == null) { // Should not happen if population > 0
            System.err.println("Warning: No best schedule found, returning best of last generation.");
            population.sort(Comparator.comparingInt(Schedule::getFitness).reversed());
            return population.get(0);
        }
        return bestScheduleOverall;
    }

    // Tournament Selection
    private static Schedule selectParentTournament(List<Schedule> population) {
        int tournamentSize = 5; // Common tournament size
        List<Schedule> tournament = new ArrayList<>();
        for (int i = 0; i < tournamentSize; i++) {
            tournament.add(population.get(random.nextInt(population.size())));
        }
        // Return the fittest individual from the tournament
        return Collections.max(tournament, Comparator.comparingInt(Schedule::getFitness));
    }

    // Random Selection (Simpler alternative)
    // private static Schedule selectParent(List<Schedule> population) {
    //     return population.get(random.nextInt(population.size()));
    // }

    /**
     * Crossover: Uniform crossover - for each slot, randomly pick the gene (subject) from parent1 or parent2.
     * This might be better than day-based crossover for preserving local structures sometimes.
     */
    static Schedule crossover(Schedule parent1,
                            Schedule parent2,
                            Map<String, String> subjectStaffMap,
                            Map<String, Boolean> isLabMap,
                            Map<String, Integer> subjectsWithPeriods,
                            Schedule otherSection) {
        Map<String, List<String>> childTimetable = new HashMap<>();
        for (String day : DAYS_OF_WEEK) {
            List<String> childDaySchedule = new ArrayList<>(PERIODS_PER_DAY);
            List<String> p1Day = parent1.getTimetable().get(day);
            List<String> p2Day = parent2.getTimetable().get(day);
            for (int i = 0; i < PERIODS_PER_DAY; i++) {
                // Randomly choose the subject for this slot from either parent
                childDaySchedule.add(random.nextBoolean() ? p1Day.get(i) : p2Day.get(i));
            }
            childTimetable.put(day, childDaySchedule);
        }
        // Creating a new Schedule object automatically calculates its fitness
        return new Schedule(childTimetable, subjectStaffMap, isLabMap, subjectsWithPeriods, otherSection);
    }


    /**
     * Mutation: Randomly swap two periods *within* a random day.
     * This simple mutation might break lab continuity, but fitness penalties should guide evolution away from it.
     */
    static void mutate(Schedule schedule) {
        Map<String, List<String>> timetable = schedule.getTimetable();
        String day = DAYS_OF_WEEK.get(random.nextInt(DAYS_OF_WEEK.size()));
        List<String> daySchedule = timetable.get(day);

        if (daySchedule.size() > 1) {
            int idx1 = random.nextInt(daySchedule.size());
            int idx2 = random.nextInt(daySchedule.size());
            // Ensure indices are different
            int attempts = 0;
            while (idx1 == idx2 && daySchedule.size() > 1 && attempts < 10) {
                 idx2 = random.nextInt(daySchedule.size());
                 attempts++;
            }

            if(idx1 != idx2) {
                Collections.swap(daySchedule, idx1, idx2);
                // Recompute fitness after mutation modifies the schedule
                schedule.recalcFitness();
            }
        }
    }

    /**
     * Utility to print a timetable.
     */
    private static void printTimetable(Schedule schedule, String section) {
        System.out.println("\nOptimized Timetable for " + section + ":");
        System.out.println("--------------------------------------------------------------------------------------");
        System.out.printf("| %-10s |", "Day");
        for (int i = 1; i <= PERIODS_PER_DAY; i++) {
            System.out.printf(" %-6s |", "P" + i);
        }
        System.out.println("\n--------------------------------------------------------------------------------------");

        Map<String, List<String>> timetable = schedule.getTimetable();
        if (timetable == null || timetable.isEmpty()) {
             System.out.println("| Timetable data is missing or invalid.                                              |");
             System.out.println("--------------------------------------------------------------------------------------");
             System.out.println("Fitness: " + schedule.getFitness() + " (Likely Invalid)");
             return;
        }


        for (String day : DAYS_OF_WEEK) {
            System.out.printf("| %-10s |", day);
            List<String> periods = timetable.get(day);
            if (periods == null || periods.size() != PERIODS_PER_DAY) {
                 System.out.printf(" Invalid schedule data for this day%n");
                 continue;
            }
            for (String period : periods) {
                 if (period == null) period = "NULL"; // Indicate errors
                String display = period.length() > 6 ? period.substring(0, 6) : period;
                System.out.printf(" %-6s |", display);
            }
            System.out.println();
        }
        System.out.println("--------------------------------------------------------------------------------------");
        System.out.println("Fitness: " + schedule.getFitness());
    }

    public static void main(String[] args) {
        Map<String, Integer> subjectsWithPeriods = new HashMap<>();
        Map<String, String> subjectStaffMap = new HashMap<>();
        Map<String, Boolean> isLabMap = new HashMap<>();

        try {
            System.out.print("Enter number of subjects (including labs): ");
            int numSubjects = scanner.nextInt();
            scanner.nextLine(); // consume newline
            if (numSubjects <= 0) {
                throw new IllegalArgumentException("Number of subjects must be positive.");
            }

            for (int i = 0; i < numSubjects; i++) {
                System.out.println("\nSubject " + (i + 1) + ":");
                System.out.print("Enter subject name (e.g., MA8351 or LAB1): ");
                String name = scanner.nextLine().trim().toUpperCase(); // Standardize name case
                if (name.isEmpty()) throw new IllegalArgumentException("Subject name cannot be empty.");
                if (subjectsWithPeriods.containsKey(name)) throw new IllegalArgumentException("Duplicate subject name: " + name);


                System.out.print("Enter number of periods per week (e.g., 4 for theory, 2 or 3 for lab): ");
                int periods = scanner.nextInt();
                scanner.nextLine(); // consume newline
                if (periods <= 0 || periods > TOTAL_PERIODS_PER_WEEK) { // Max is total possible periods
                    throw new IllegalArgumentException("Periods per week must be positive and reasonable.");
                }

                System.out.print("Is this subject a theory or lab? (theory/lab): ");
                String type = scanner.nextLine().trim().toLowerCase();
                if (!type.equals("theory") && !type.equals("lab")) {
                    throw new IllegalArgumentException("Please enter 'theory' or 'lab'.");
                }
                boolean isLab = type.equals("lab");

                 // Labs usually have duration 2 or 3
                 if (isLab && (periods < 2 || periods > 4)) { // Allow labs up to 4 periods? Adjust if needed.
                     System.out.println("Warning: Lab periods are typically 2 or 3. Ensure " + periods + " is correct.");
                 }


                System.out.print("Enter staff name (e.g., Dr. Smith): ");
                String staff = scanner.nextLine().trim();
                if (staff.isEmpty()) {
                    throw new IllegalArgumentException("Staff name cannot be empty.");
                }

                subjectsWithPeriods.put(name, periods);
                subjectStaffMap.put(name, staff);
                isLabMap.put(name, isLab);
            }

            // *** CRITICAL VALIDATION: Total periods must be exactly TOTAL_PERIODS_PER_WEEK (40) ***
            int totalSubjectPeriods = subjectsWithPeriods.values().stream().mapToInt(Integer::intValue).sum();
            if (totalSubjectPeriods != TOTAL_PERIODS_PER_WEEK) {
                throw new IllegalArgumentException(
                        "Input Error: Total periods for all subjects must be exactly " + TOTAL_PERIODS_PER_WEEK +
                        ". You entered a total of " + totalSubjectPeriods + "."
                );
            }

            System.out.println("\nStarting Genetic Algorithm...");

            // Generate timetable for Section A
             System.out.println("Generating Section A...");
            Schedule scheduleA = runGeneticAlgorithm(subjectsWithPeriods, subjectStaffMap, isLabMap, null);

            // Generate timetable for Section B, penalizing collisions with Section A
             System.out.println("\nGenerating Section B (considering Section A for staff)...");
            Schedule scheduleB = runGeneticAlgorithm(subjectsWithPeriods, subjectStaffMap, isLabMap, scheduleA);

            // Print timetables
            printTimetable(scheduleA, "Section A");
            printTimetable(scheduleB, "Section B");

        } catch (InputMismatchException e) {
            System.err.println("Invalid input. Please enter numeric values where required.");
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
             e.printStackTrace(); // Print stack trace for unexpected errors
        } finally {
            scanner.close();
        }
    }
}