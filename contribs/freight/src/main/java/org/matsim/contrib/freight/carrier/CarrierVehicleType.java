package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * The carrier vehicle type.
 * 
 * I decided to put vehicle cost information into the type (which is indeed not a physical attribute of the type). Thus physical and
 * non physical attributes are used. This is likely to be changed in future.
 * 
 * @author sschroeder
 *
 */
public class CarrierVehicleType {

	/**
	 * A builder building the type.
	 * 
	 * @author sschroeder
	 *
	 */
	public static class Builder {
		
		/**
		 * Returns a new instance of builder initialized with the typeId.
		 * 
		 * The defaults are [fix=0.0][perDistanceUnit=1.0][perTimeUnit=0.0].
		 * 
		 * @param typeId
		 * @return a type builder
		 */
		public static Builder newInstance(Id<VehicleType> typeId){
			return new Builder(typeId);
		}
		
//		/**
//		 * Returns a new instance of builder initialized with the typeId and the values the given from existing CarrierVehicleType.
//		 *
//		 * Can be used for create a new, modified CarrierVehicleType basing on an existing one.
//		 * Values can be changed within the builder afterwards.
//		 *
//		 * @param carrierVehicleType
//		 * @param typeId
//		 * @return a type builder
//		 */
//		public static Builder newInstance(Id<VehicleType> typeId, CarrierVehicleType carrierVehicleType){
//			return new Builder(typeId)
//					.setDescription(carrierVehicleType.getDescription())
//					.setEngineInformation(carrierVehicleType.getEngineInformation())
//					.setCapacity(carrierVehicleType.getCarrierVehicleCapacity())
//					.setMaxVelocity(carrierVehicleType.getMaximumVelocity())
//					.setVehicleCostInformation(carrierVehicleType.getVehicleCostInformation());
//		}
		
		private VehicleType vehicleType;

		private Builder(Id<VehicleType> typeId){
			this.vehicleType = VehicleUtils.getFactory().createVehicleType( typeId ) ;
		}
		
		/**
		 * Sets fixed costs of vehicle.
		 * 
		 * <p>By default it is 0.
		 * @param fix
		 * @return
		 */
		public Builder setFixCost(double fix){
			this.vehicleType.getCostInformation().setFixedCost( fix ) ;
			return this;
		}
		
		/**
		 * Sets costs per distance-unit.
		 * 
		 * <p>By default it is 1.
		 * 
		 * @param perDistanceUnit
		 * @return
		 */
		public Builder setCostPerDistanceUnit(double perDistanceUnit){
			this.vehicleType.getCostInformation().setCostsPerMeter( perDistanceUnit ) ;
			return this;
		}
		
		/**
		 * Sets costs per time-unit.
		 * 
		 * <p>By default it is 0.
		 * 
		 * @param perTimeUnit
		 * @return
		 */
		public Builder setCostPerTimeUnit(double perTimeUnit){
			this.vehicleType.getCostInformation().setCostsPerSecond( perTimeUnit ) ;
			return this;
		}
		
		/**
		 * Sets description.
		 * 
		 * @param description
		 * @return this builder
		 */
		public Builder setDescription(String description){
			this.vehicleType.setDescription( description ) ;
			return this;
		}
		
		/**
		 * Sets the capacity of vehicle-type.
		 * 
		 * <p>By defaul the capacity is 0.
		 * 
		 * @param capacity
		 * @return this builder
		 */
		public Builder setCapacity(int capacity){
			this.vehicleType.getCapacity().setOther( capacity );
			return this;
		}
		
		/**
		 * Builds the type.
		 * 
		 * @return {@link CarrierVehicleType}
		 */
		public VehicleType build(){
//			return new CarrierVehicleType(this);
			return this.vehicleType ;
		}

		/**
		 * Sets {@link VehicleCostInformation}
		 * 
		 * <p>The defaults are [fix=0.0][perDistanceUnit=1.0][perTimeUnit=0.0].
		 * 
		 * @param info
		 * @return this builder
		 */
		public Builder setVehicleCostInformation(VehicleCostInformation info) {
			this.vehicleType.getCostInformation().setCostsPerSecond( info.getPerTimeUnit() ) ;
			this.vehicleType.getCostInformation().setCostsPerMeter( info.getPerDistanceUnit() ) ;
			this.vehicleType.getCostInformation().setFixedCost( info.getFix() ) ;
			return this;
		}

		/**
		 * Sets {@link EngineInformation}
		 * 
		 * @param engineInfo
		 * @return this builder
		 */
		public Builder setEngineInformation(EngineInformation engineInfo) {
			this.vehicleType.getEngineInformation().setFuelConsumption( engineInfo.getFuelConsumption() );
			this.vehicleType.getEngineInformation().setFuelType( engineInfo.getFuelType() );
			return this;
		}

		public Builder setMaxVelocity(double veloInMeterPerSeconds) {
			this.vehicleType.setMaximumVelocity( veloInMeterPerSeconds ) ;
			return this;
		}
	}
	
	public static class VehicleCostInformation {

		private double fix;
		private double perDistanceUnit;
		private double perTimeUnit;

		public VehicleCostInformation(double fix, double perDistanceUnit, double perTimeUnit) {
			super();
			this.fix = fix;
			this.perDistanceUnit = perDistanceUnit;
			this.perTimeUnit = perTimeUnit;
		}
		
		public double getFix() {
			return fix;
		}

		public double getPerDistanceUnit() {
			return perDistanceUnit;
		}

		public double getPerTimeUnit() {
			return perTimeUnit;
		}

	}

	private VehicleCostInformation vehicleCostInformation;

	private int capacity;
	
//	private CarrierVehicleType(Builder builder){
//		super(new VehicleTypeImpl(builder.typeId));
//		this.vehicleCostInformation = new VehicleCostInformation(builder.fix, builder.perDistanceUnit, builder.perTimeUnit);
//		if(builder.engineInfo != null) super.setEngineInformation(builder.engineInfo);
//		if(builder.description != null) super.setDescription(builder.description);
//		capacity = builder.capacity;
//		super.setMaximumVelocity(builder.maxVeloInMeterPerSeconds);
//	}

	/**
	 * Returns the cost values for this vehicleType.
	 * 
	 * If cost values are not explicitly set, the defaults are [fix=0.0][perDistanceUnit=1.0][perTimeUnit=0.0].
	 * 
	 * @return vehicleCostInformation
	 */
	public VehicleCostInformation getVehicleCostInformation() {
		return vehicleCostInformation;
	}
	

	/**
	 * Returns the capacity of carrierVehicleType.
	 * 
	 * <p>This might be replaced in future by a more complex concept of capacity (considering volume and different units).
	 * 
	 * @return integer
	 */
	public int getCarrierVehicleCapacity(){
		return capacity;
	}
	
}
