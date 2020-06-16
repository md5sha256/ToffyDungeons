package toffydungeons.toffydungeons.API;

import com.sk89q.worldedit.Vector;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import toffydungeons.toffydungeons.DungeonDesign.DungeonRoomDesign;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This is the class that organises the layout of every dungeon, it has an arraylist of rooms which each have their neighbouring
 * rooms stored as well as their position. The starting room is the room that the player initialy spawns in.
 */
public class DungeonRoomLayout {

    private ArrayList<DungeonRoom> rooms;
    private DungeonRoom startingRoom;
    private ArrayList<DungeonRoom> builtRooms;
    public String dungeonName;
    private int buildTime;
    private int[] cachedView;
    private Location builtLoc;
    private  ArrayList<String> roomDatas;

    public DungeonRoomLayout() {
        this.rooms = new ArrayList<>();
    }

    public ArrayList<DungeonRoom> getRooms() {
        return rooms;
    }

    public void addRoom(DungeonRoom room) {
        if (!validateRoom(room)) {
            this.rooms.add(room);
        }
    }

    public int[] getCachedView() {
        return cachedView;
    }

    public void setCachedView(int[] cachedView) {
        this.cachedView = cachedView;
    }

    public void setStartingRoom(DungeonRoom room) {
        this.startingRoom = room;
        int[] newSides = room.getBlockedSides();
        newSides[2] = 1;
        if (room.getBehind() != null) {
            room.getBehind().setForward(null);
            room.setBehind(null);
        }
        room.setBlockedSides(newSides);
    }

    public DungeonRoom getStartingRoom() {
        return startingRoom;
    }

    public void removeRoomFromPosition(int[] position) {
        for (DungeonRoom room : this.rooms) {
            if (Arrays.equals(room.getPosition(), position) && !this.getStartingRoom().equals(room)) {
                this.safeRemoveRoom(room);
                break;
            }
        }
    }

    public DungeonRoom getRoomFromPosition(int[] position) {
        for (DungeonRoom room : this.rooms) {
            if (Arrays.equals(room.getPosition(), position)) {
                return room;
            }
        } return null;
    }

    public boolean validateRoom(DungeonRoom room) {
        return this.rooms.contains(room);
    }

    public ArrayList<int[]> getPositions() {
        ArrayList<int[]> positions = new ArrayList<>();
        for (DungeonRoom room : rooms) {
            positions.add(room.getPosition());
        }
        return positions;
    }

    public void updateBorders() {
        for (DungeonRoom room : this.rooms) {
            for (DungeonRoom roomToCompare : this.rooms) {
                if (room.getPosition()[0] + 1 == roomToCompare.getPosition()[0] && roomToCompare.getPosition()[1] == room.getPosition()[1] && room.getBlockedSides()[1] ==0 && roomToCompare.getBlockedSides()[3] ==0) {
                    room.setRight(roomToCompare);
                    roomToCompare.setLeft(room);
                } else if (room.getPosition()[0] - 1 == roomToCompare.getPosition()[0] && roomToCompare.getPosition()[1] == room.getPosition()[1] && room.getBlockedSides()[3] ==0 && roomToCompare.getBlockedSides()[1] ==0) {
                    room.setLeft(roomToCompare);
                    roomToCompare.setRight(room);
                } else if (room.getPosition()[1] + 1 == roomToCompare.getPosition()[1] && roomToCompare.getPosition()[0] == room.getPosition()[0] && room.getBlockedSides()[2] ==0 && roomToCompare.getBlockedSides()[0] ==0) {
                    room.setBehind(roomToCompare);
                    roomToCompare.setForward(room);
                } else if (room.getPosition()[1] - 1 == roomToCompare.getPosition()[1] && roomToCompare.getPosition()[0] == room.getPosition()[0] && room.getBlockedSides()[0] ==0 && roomToCompare.getBlockedSides()[2] ==0) {
                    room.setForward(roomToCompare);
                    roomToCompare.setBehind(room);
                }
            }
        }
    }

    public void safeRemoveRoom(DungeonRoom room) {
        if (!this.getStartingRoom().equals(room)) {
            if (room.getForward() != null)
                room.getForward().setBehind(null);
            if (room.getBehind() != null)
                room.getBehind().setForward(null);
            if (room.getLeft() != null)
                room.getLeft().setRight(null);
            if (room.getRight() != null)
                room.getRight().setLeft(null);
            this.rooms.remove(room);
        }
    }

    public void generateBuild(Location location) {
        this.builtLoc = location;
        this.roomDatas = new ArrayList<>();
        builtRooms = new ArrayList<>();
        buildTime = 0;
        new GenerateBuild("null", startingRoom, location, this.roomDatas).run();
    }

    private void checkRegister() {
        if (builtRooms.size() == rooms.size()) {
            FileSaving.saveFile("active_dungeons", "active_dungeons" + File.separator +  "noread.check");
            int adder = 1;
            String name = ("Dungeon_" + (FileSaving.filesInDirectory("active_dungeons").size() + adder));
            while (FileSaving.folderContainsFile("active_dungeons", name + ".adungeon")) {
                adder +=1;
                name = ("Dungeon_" + (FileSaving.filesInDirectory("active_dungeons").size() + adder));
            }
            FileSaving.saveFile("active_dungeons", "Active_Dungeons" + File.separator + name + ".adungeon");
            ArrayList<String> savedData = new ArrayList<>();
            savedData.add(dungeonName);
            savedData.addAll(this.roomDatas);
            savedData.add(builtLoc.getWorld().getName() + "," +  builtLoc.getBlockX() + "," +  builtLoc.getBlockY() + "," +  builtLoc.getBlockZ());
            FileSaving.writeFile("active_dungeons" + File.separator + name + ".adungeon", savedData);
            FileSaving.deleteFile("active_dungeons" + File.separator + "noread.check");
        }
    }

    public static void unloadRoom(String roomToRemove) {
        int buildTime = 0;
        for (String line : FileSaving.readLines("active_dungeons" + File.separator + roomToRemove + ".adungeon")) {
            if (line.split(",").length==11) {
                String[] splitLine  = line.split(",");
                Location loc1 = new Location(Bukkit.getWorld(splitLine[1]), Integer.valueOf(splitLine[5]), Integer.valueOf(splitLine[6]), Integer.valueOf(splitLine[7]));
                Location loc2 = new Location(Bukkit.getWorld(splitLine[1]), Integer.valueOf(splitLine[8]), Integer.valueOf(splitLine[9]), Integer.valueOf(splitLine[10]));
                UnloadBuild unloader = new UnloadBuild(loc1, loc2);
                Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getPluginManager().getPlugin("ToffyDungeons"), unloader, 20 * buildTime);
                buildTime += 1;
            } else {
                System.out.println(line.split(",").length);;
            }
        }
    }

    public static DungeonRoomLayout deserialise(ArrayList<String> serialisedData) {
        DungeonRoomLayout layout = new DungeonRoomLayout();
        for (String line : serialisedData) {
            if (line.contains("start:")) {
                line = line.substring(6);
                int[] newPos = new int[]{ Integer.valueOf(line.split(",")[0]),Integer.valueOf(line.split(",")[1]) };
                DungeonRoom newRoom = new DungeonRoom("ExampleRoom", newPos);
                newRoom.setBlockedSides(new int[]{Integer.valueOf(line.split(",")[3]),Integer.valueOf(line.split(",")[4]), Integer.valueOf(line.split(",")[5]), Integer.valueOf(line.split(",")[6])});
                layout.addRoom(newRoom);
                newRoom.setSchematicFile(line.split(",")[2]);
                layout.setStartingRoom(newRoom);
            } else if (line.contains("position:")) {
                line = line.substring(9);
                DungeonRoom newRoom = new DungeonRoom("ExampleRoom", new int[]{Integer.valueOf(line.split(",")[0]),Integer.valueOf(line.split(",")[1]) });
                newRoom.setBlockedSides(new int[]{Integer.valueOf(line.split(",")[3]),Integer.valueOf(line.split(",")[4]), Integer.valueOf(line.split(",")[5]), Integer.valueOf(line.split(",")[6])});
                layout.addRoom(newRoom);
                newRoom.setSchematicFile(line.split(",")[2]);
            }
        } layout.updateBorders();
        return layout;
    }

    private boolean isRoomBuild(DungeonRoom room) {
        return builtRooms.contains(room);
    }

    public static class UnloadBuild extends BukkitRunnable {

        private Location loc1;
        private Location loc2;

        public UnloadBuild(Location loc1, Location loc2) {
            this.loc1 = new Location (loc1.getWorld(), Math.min(loc1.getBlockX(), loc2.getBlockX()), Math.min(loc1.getBlockY(), loc2.getBlockY()), Math.min(loc1.getBlockZ(), loc2.getBlockZ()));
            this.loc2 = new Location (loc1.getWorld(), Math.max(loc1.getBlockX(), loc2.getBlockX()), Math.max(loc1.getBlockY(), loc2.getBlockY()), Math.max(loc1.getBlockZ(), loc2.getBlockZ()));
        }

        @Override
        public void run() {
            CalebWorldEditAPI.setBlock(loc1, loc2, Material.AIR);
        }
    }

    public class GenerateBuild extends BukkitRunnable {

        private String direction;
        private DungeonRoom room;
        private Location coordinates;
        private ArrayList<String> posData;

        public GenerateBuild(String direction, DungeonRoom room, Location coordinates, ArrayList<String> posData) {
            this.direction = direction;
            this.room = room;
            this.coordinates = coordinates;
            this.posData = posData;
        }

        /**
         * HOLY SHIT this method, I KNOW ITS A MESS OK? This took me a long time to make (at the early hours of the morning)
         * and in all honesty I am scared to optimise it, esentially this is a recursive runnable task (creates new runnables of itself)
         * and it takes a starting room and builds every adjacent room, repeating for every room it constructs (makes the room in front
         * and then does the 3 other directios of that room). It ignores any rooms already created. If any developers want to make this
         * highly inefficient code better please please go ahead but I am sleeping and forgetting I made this.
         */
        @Override
        public void run() {
            if (!isRoomBuild(room)) {
                if (buildTime >= 1) {
                    buildTime -= 1;
                }
                builtRooms.add(room);
                String saveData = room.getSchematicFile() + ",";
                saveData += coordinates.getWorld().getName() + "," + coordinates.getBlockX() + "," + coordinates.getBlockY() + "," + coordinates.getBlockZ();
                saveData += DungeonRoomDesign.calculateCoords(room.getSchematicFile(), coordinates, direction);
                posData.add(saveData);
                checkRegister();
                File roomStats = new File(Bukkit.getPluginManager().getPlugin("ToffyDungeons").getDataFolder() + File.separator + "rooms" + File.separator + room.getSchematicFile() + ".placement");
                int[] directions = new int[12];
                GenerateBuild forward = null;
                GenerateBuild right = null;
                GenerateBuild left = null;
                GenerateBuild back = null;
                try {
                    FileReader fr = new FileReader(roomStats);
                    BufferedReader br = new BufferedReader(fr);
                    String line;
                    int i = 0;
                    while ((line=br.readLine()) != null) {
                        for (String current : new String[]{"NORTH:", "EAST:", "SOUTH:", "WEST:"}) {
                            if (line.contains(current)) {
                                directions[i] = Integer.valueOf(line.split(current)[1].split(",")[0]);
                                directions[i + 1] = Integer.valueOf(line.split(current)[1].split(",")[1]);
                                directions[i + 2] = Integer.valueOf(line.split(current)[1].split(",")[2]);
                                i +=3;
                            }
                        }
                    }
                    Vector offset = new Vector(directions[6], directions[7], directions[8]);
                    if (direction.equals("forward") || direction.equals("null")) {
                        offset = new Vector(-directions[6], directions[7], -directions[8]);
                        CalebWorldEditAPI.tryLoadSchem(room.getSchematicFile(), coordinates, offset);
                        if (room.getForward() != null) {
                            Location newCoordinates = new Location(coordinates.getWorld(), coordinates.getBlockX(), coordinates.getBlockY(), coordinates.getBlockZ());
                            newCoordinates.setX(newCoordinates.getBlockX() + directions[0]);
                            newCoordinates.setY(newCoordinates.getBlockY() + directions[1]);
                            newCoordinates.setZ(newCoordinates.getBlockZ() + directions[2]);
                            forward = new GenerateBuild("forward", room.getForward(), newCoordinates, posData);
                        }
                        if (room.getBehind() != null && !direction.equals("forward")) {
                            Location newCoordinates = new Location(coordinates.getWorld(), coordinates.getBlockX(), coordinates.getBlockY(), coordinates.getBlockZ());
                            newCoordinates.setX(newCoordinates.getBlockX() + 1 );
                            newCoordinates.setZ(newCoordinates.getBlockZ() + directions[6]);
                            back = new GenerateBuild("behind", room.getBehind(), newCoordinates, posData);
                        }
                        if (room.getRight() != null) {
                            Location newCoordinates = new Location(coordinates.getWorld(), coordinates.getBlockX(), coordinates.getBlockY(), coordinates.getBlockZ());
                            newCoordinates.setX(newCoordinates.getBlockX() + directions[3]);
                            newCoordinates.setY(newCoordinates.getBlockY() + directions[4]);
                            newCoordinates.setZ(newCoordinates.getBlockZ() + directions[5]);
                            right = new GenerateBuild("right", room.getRight(), newCoordinates, posData);
                        }
                        if (room.getLeft() != null) {
                            Location newCoordinates = new Location(coordinates.getWorld(), coordinates.getBlockX(), coordinates.getBlockY(), coordinates.getBlockZ());
                            newCoordinates.setX(newCoordinates.getBlockX() + directions[9]);
                            newCoordinates.setY(newCoordinates.getBlockY() + directions[10]);
                            newCoordinates.setZ(newCoordinates.getBlockZ() + directions[11]);
                            left = new GenerateBuild("left", room.getLeft(), newCoordinates, posData);
                        }
                    }
                    if (direction.equals("behind")) {
                        CalebWorldEditAPI.tryLoadSchem(room.getSchematicFile(), coordinates, 180, offset);
                        if (room.getBehind() != null) {
                            Location newCoordinates = new Location(coordinates.getWorld(), coordinates.getBlockX(), coordinates.getBlockY(), coordinates.getBlockZ());
                            newCoordinates.setX(newCoordinates.getBlockX() - directions[0]);
                            newCoordinates.setY(newCoordinates.getBlockY() + directions[1]);
                            newCoordinates.setZ(newCoordinates.getBlockZ() - directions[2]);
                            back = new GenerateBuild("behind", room.getBehind(), newCoordinates, posData);
                        }
                        if (room.getRight() != null) {
                            Location newCoordinates = new Location(coordinates.getWorld(), coordinates.getBlockX(), coordinates.getBlockY(), coordinates.getBlockZ());
                            newCoordinates.setX(newCoordinates.getBlockX() - directions[9]);
                            newCoordinates.setY(newCoordinates.getBlockY() + directions[10]);
                            newCoordinates.setZ(newCoordinates.getBlockZ() - directions[11]);
                            right = new GenerateBuild("right", room.getRight(), newCoordinates, posData);
                        }
                        if (room.getLeft() != null) {
                            Location newCoordinates = new Location(coordinates.getWorld(), coordinates.getBlockX(), coordinates.getBlockY(), coordinates.getBlockZ());
                            newCoordinates.setX(newCoordinates.getBlockX() - directions[3]);
                            newCoordinates.setY(newCoordinates.getBlockY() + directions[4]);
                            newCoordinates.setZ(newCoordinates.getBlockZ() - directions[5]);
                            left = new GenerateBuild("left", room.getLeft(), newCoordinates, posData);
                        }
                    }
                    if (direction.equals("left")) {
                        CalebWorldEditAPI.tryLoadSchem(room.getSchematicFile(), coordinates, 270, offset);
                        if (room.getForward() != null) {
                            Location newCoordinates = new Location(coordinates.getWorld(), coordinates.getBlockX(), coordinates.getBlockY(), coordinates.getBlockZ());
                            newCoordinates.setX(newCoordinates.getBlockX() + directions[5]);
                            newCoordinates.setY(newCoordinates.getBlockY() + directions[4]);
                            newCoordinates.setZ(newCoordinates.getBlockZ() - directions[3]);
                            forward = new GenerateBuild("forward", room.getForward(), newCoordinates, posData);
                        }
                        if (room.getBehind() != null) {
                            Location newCoordinates = new Location(coordinates.getWorld(), coordinates.getBlockX(), coordinates.getBlockY(), coordinates.getBlockZ());
                            newCoordinates.setX(newCoordinates.getBlockX() + directions[11] );
                            newCoordinates.setY(newCoordinates.getBlockY() + directions[10]);
                            newCoordinates.setZ(newCoordinates.getBlockZ() - directions[9]);
                            back = new GenerateBuild("behind", room.getBehind(), newCoordinates, posData);
                        }
                        if (room.getLeft() != null) {
                            Location newCoordinates = new Location(coordinates.getWorld(), coordinates.getBlockX(), coordinates.getBlockY(), coordinates.getBlockZ());
                            newCoordinates.setX(newCoordinates.getBlockX() + directions[2]);
                            newCoordinates.setY(newCoordinates.getBlockY() + directions[1]);
                            newCoordinates.setZ(newCoordinates.getBlockZ() - directions[0]);
                            left = new GenerateBuild("left", room.getLeft(), newCoordinates, posData);
                        }
                    }
                    if (direction.equals("right")) {
                        CalebWorldEditAPI.tryLoadSchem(room.getSchematicFile(), coordinates, 90, offset);

                        if (room.getForward() != null) {
                            Location newCoordinates = new Location(coordinates.getWorld(), coordinates.getBlockX(), coordinates.getBlockY(), coordinates.getBlockZ());
                            newCoordinates.setX(newCoordinates.getBlockX() - directions[11]);
                            newCoordinates.setY(newCoordinates.getBlockY() + directions[10]);
                            newCoordinates.setZ(newCoordinates.getBlockZ() + directions[9]);
                            forward = new GenerateBuild("forward", room.getForward(), newCoordinates, posData);
                        }

                        if (room.getBehind() != null) {
                            Location newCoordinates = new Location(coordinates.getWorld(), coordinates.getBlockX(), coordinates.getBlockY(), coordinates.getBlockZ());
                            newCoordinates.setX(newCoordinates.getBlockX() - directions[5] );
                            newCoordinates.setY(newCoordinates.getBlockY() + directions[4]);
                            newCoordinates.setZ(newCoordinates.getBlockZ() + directions[3]);
                            back = new GenerateBuild("behind", room.getBehind(), newCoordinates, posData);
                        }

                        if (room.getRight() != null) {
                            Location newCoordinates = new Location(coordinates.getWorld(), coordinates.getBlockX(), coordinates.getBlockY(), coordinates.getBlockZ());
                            newCoordinates.setX(newCoordinates.getBlockX() - directions[2]);
                            newCoordinates.setY(newCoordinates.getBlockY() + directions[1]);
                            newCoordinates.setZ(newCoordinates.getBlockZ() + directions[0]);
                            right = new GenerateBuild("right", room.getRight(), newCoordinates, posData);
                        }
                    }
                    if (forward != null) {
                        buildTime +=1;
                        Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getPluginManager().getPlugin("ToffyDungeons"), forward, 20 * buildTime);
                    }
                    if (back != null) {
                        buildTime +=1;
                        Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getPluginManager().getPlugin("ToffyDungeons"), back, 20 * buildTime);
                    }
                    if (right != null) {
                        buildTime +=1;
                        Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getPluginManager().getPlugin("ToffyDungeons"), right, 20 * buildTime);
                    }
                    if (left != null) {
                        buildTime +=1;
                        Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getPluginManager().getPlugin("ToffyDungeons"), left, 20 * buildTime);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }
    }

}
