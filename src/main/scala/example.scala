package com.laodmods.StandaloneActuator

import cpw.mods.fml.common
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.FMLLog
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.network.{IGuiHandler, NetworkRegistry}
import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side

import org.apache.logging.log4j.Logger

import net.minecraft.block.Block
import net.minecraft.block.BlockContainer
import net.minecraft.block.BlockDirt
import net.minecraft.block.ITileEntityProvider
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP, InventoryPlayer}
import net.minecraft.init.Blocks
import net.minecraft.inventory.{Container, IInventory, Slot}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.server.MinecraftServer
import net.minecraft.tileentity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraft.world.WorldServer

import com.laodmods.util.LocalFakePlayer
import com.laodmods.util.LocalFakePlayerFactory

class MyGH extends IGuiHandler{
  NetworkRegistry.INSTANCE.registerGuiHandler(StandaloneActuator.instance, this)

  def getServerGuiElement(id: Int, p: EntityPlayer, w: World, x: Int, y: Int, z: Int): Object = {
    new ActuatorContainer(p,w.getTileEntity(x,y,z).asInstanceOf[MyTE])
  }
  def getClientGuiElement(id: Int, p: EntityPlayer, w: World, x: Int, y: Int, z: Int): Object = {
    new ActuatorGUI(new ActuatorContainer(p,w.getTileEntity(x,y,z).asInstanceOf[MyTE]))
  }
}

class ActuatorContainer(p: EntityPlayer, te: MyTE) extends Container{

  addSlotToContainer(new Slot(p.inventory, 0, 10, 10))
  addSlotToContainer(new Slot(te, 0, 38, 10))

  def canInteractWith(p: EntityPlayer): Boolean = {
    true
  }
}

@SideOnly(Side.CLIENT)
class ActuatorGUI(c: Container) extends GuiContainer(c){
  val texture: ResourceLocation = new ResourceLocation("textures/gui/actuator.png")
  def drawGuiContainerBackgroundLayer(x: Float, y: Int, z: Int){
    mc.getTextureManager().bindTexture(texture)
    val xo = (width - xSize)/2
    val yo = (height - ySize)/2
    drawTexturedModalRect(xo, yo, 0, 0, xSize, ySize)
  }
}

class MyTE extends TileEntity with IInventory{
  var neighbors: List[(Int, Int, Block, TileEntity)] = null
  var ticks: Int = 0
  var inventory: Array[ItemStack] = new Array[ItemStack](1)

  override def updateEntity{
    if (ticks % 10 == 0 || neighbors == null) getNeighbors
    if (ticks == 19) ticks = 0
    if (ticks % 5 == 0) {
      val p = LocalFakePlayerFactory.getMinecraft(Minecraft.getMinecraft.getIntegratedServer.worldServerForDimension(0))
      val ds = new ItemStack(Blocks.dirt)
      p.inventory.setInventorySlotContents(0,ds)
      neighbors.foreach{ n => n._3.onBlockActivated(worldObj, n._1, yCoord, n._2, p, 0, 0.1f, 0.0f, 0.0f)}
    }
    ticks += 1
  }

  def getNeighbors{
    neighbors = List(List(xCoord-1,zCoord), List(xCoord+1,zCoord), List(xCoord,zCoord-1), List(xCoord,zCoord+1))
      .map{ c => ( c(0), c(1), worldObj.getBlock(c(0), yCoord, c(1)), worldObj.getTileEntity(c(0),yCoord,c(1)) ) }
  }

  def closeInventory(){
  }

  def decrStackSize(x$1: Int,x$2: Int): net.minecraft.item.ItemStack = {
    inventory(0).stackSize -= 1
    inventory(0)
  }

  def getInventoryName(): String = "Standalone Actuator"

  def getInventoryStackLimit(): Int = 64

  def getSizeInventory(): Int = 1

  def getStackInSlot(x$1: Int): net.minecraft.item.ItemStack = {
    inventory(0)
  }

  def getStackInSlotOnClosing(x$1: Int): net.minecraft.item.ItemStack = {
    null
  }

  def hasCustomInventoryName(): Boolean = true

  def isItemValidForSlot(x$1: Int,x$2: net.minecraft.item.ItemStack): Boolean = true

  def isUseableByPlayer(x$1: net.minecraft.entity.player.EntityPlayer): Boolean = true

  def openInventory(){}

  def setInventorySlotContents(x$1: Int,x$2: net.minecraft.item.ItemStack){
    inventory(0) = x$2
    FMLLog.info("%s",inventory)
  }
}

class ActuatorBlock(mat: Material) extends BlockContainer(mat: Material){
  var te: MyTE = null

  def createNewTileEntity(w: World, i: Int) : TileEntity = {
    te = new MyTE
    te
  }

  override def onBlockAdded(w: World, x: Int, y: Int, z: Int){
    te.getNeighbors
  }

  override def onBlockActivated(w: World, x: Int, y: Int, z: Int, p: EntityPlayer, side: Int, xo: Float, yo: Float, zo: Float): Boolean = {
    StandaloneActuator.instance.log.info("Activated")
    if (!w.isRemote) p.openGui(StandaloneActuator.instance, 1, w, x, y, z)
    true
  }
}

@Mod(modid = "StandaloneActuator", name = "Standalone Actuator", version = "0.0.1", modLanguage = "scala")
object StandaloneActuator {
  var log: Logger = null
  var instance = this

  @EventHandler
  def preInit(e: FMLPreInitializationEvent) {
    log = e.getModLog
  }

  @EventHandler
  def init(e: FMLInitializationEvent) {
    new MyGH()
    val b = new ActuatorBlock(Material.rock)
                  .setHardness(1.0f)
                  .setStepSound(Block.soundTypeStone)
                  .setBlockName("StandaloneActuator")
                  .setCreativeTab(CreativeTabs.tabBlock)
    val t = new MyTE
    GameRegistry.registerBlock(b, "StandaloneActuator")
    GameRegistry.registerTileEntity(t.getClass, "actuator")
    log.info("ActuatorBlock ID: " + Block.getIdFromBlock(b).toString)
  }

  @EventHandler
  def postInit(e: FMLPostInitializationEvent) {
  }
}
