import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { IDatabaseDriver } from './database-driver.model';
import { serializeAxiosError } from 'app/shared/reducers/reducer.utils';

const initialState = {
  loading: false,
  errorMessage: null,
  drivers: [] as ReadonlyArray<IDatabaseDriver>,
  uploading: false,
};

export type DatabaseDriverState = Readonly<typeof initialState>;

// Actions

export const getDrivers = createAsyncThunk(
  'databaseDriver/fetch_drivers',
  async ({ vendorId, versionId, driverType }: { vendorId?: number; versionId?: number; driverType?: string }) => {
    let requestUrl = 'api/database-drivers?';
    if (vendorId) requestUrl += `vendorId=${vendorId}&`;
    if (versionId) requestUrl += `versionId=${versionId}&`;
    if (driverType) requestUrl += `driverType=${driverType}&`;
    return axios.get<IDatabaseDriver[]>(requestUrl);
  },
  { serializeError: serializeAxiosError },
);

export const uploadDriver = createAsyncThunk(
  'databaseDriver/upload_driver',
  async (formData: FormData) => {
    return axios.post<IDatabaseDriver>('api/database-drivers/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
  { serializeError: serializeAxiosError },
);

export const DatabaseDriverSlice = createSlice({
  name: 'databaseDriver',
  initialState: initialState as DatabaseDriverState,
  reducers: {
    reset: () => initialState,
  },
  extraReducers(builder) {
    builder
      .addCase(getDrivers.pending, state => {
        state.loading = true;
        state.errorMessage = null;
      })
      .addCase(getDrivers.fulfilled, (state, action) => {
        state.loading = false;
        state.drivers = action.payload.data;
      })
      .addCase(getDrivers.rejected, (state, action) => {
        state.loading = false;
        state.errorMessage = action.error.message || null;
      })
      .addCase(uploadDriver.pending, state => {
        state.uploading = true;
        state.errorMessage = null;
      })
      .addCase(uploadDriver.fulfilled, (state, action) => {
        state.uploading = false;
        // Add to drivers list
        state.drivers = [...state.drivers, action.payload.data];
      })
      .addCase(uploadDriver.rejected, (state, action) => {
        state.uploading = false;
        state.errorMessage = action.error.message || null;
      });
  },
});

export const { reset } = DatabaseDriverSlice.actions;

// Reducer
export default DatabaseDriverSlice.reducer;
